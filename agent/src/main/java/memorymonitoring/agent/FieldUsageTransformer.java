package memorymonitoring.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import static java.lang.constant.ConstantDescs.*;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import static memorymonitoring.agent.RuntimeApiHelper.*;

final class FieldUsageTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(Module           module,
                            ClassLoader      loader,
                            String           className,
                            Class<?>         classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[]           classfileBuffer)
            throws IllegalClassFormatException {

        // TODO make configurable (via commandline arg?)
        if (!className.startsWith("memorymonitoring/example")) {
            return null;
        }

        ClassFile classFile = ClassFile.of();
        ClassModel classModel = classFile.parse(classfileBuffer);

        return classFile.transformClass(classModel, ClassTransform.transformingMethodBodies(
                (CodeBuilder codeBuilder, CodeElement codeElement) -> {
            if (codeElement instanceof FieldInstruction fieldInstruction) {
                FieldRefEntry fieldRefEntry = fieldInstruction.field();
                ClassEntry owningClass = fieldRefEntry.owner();
                Utf8Entry fieldName = fieldRefEntry.name();
                ClassDesc fieldType = fieldRefEntry.typeSymbol();

                switch (fieldInstruction.opcode()) {
                    case Opcode.GETFIELD:
                        // getfield: [..., objectRef] -> [..., fieldValue]

                        // Operand stack:
                        // [..., objectRef]
                        codeBuilder.dup();
                        // [..., objectRef, objectRef]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., objectRef, objectRef, declaringClass]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., objectRef, objectRef, declaringClass, fieldName]
                        readAccess(codeBuilder);
                        // [..., objectRef, objectRef, declaringClass, fieldName, Access.READ]
                        invokeLogFieldAccess(codeBuilder);
                        // [..., objectRef]
                        codeBuilder.with(codeElement);
                        // [..., fieldValue]
                        break;
                    case Opcode.PUTFIELD:
                        // TODO inside construtor bodies before the super constructor call, should these putfield instructions be instrumented?
                        // TODO probably not, because monitoring will always result in a violation --> test this hypothesis.
                        // TODO consider alternative: have some way where we can have a wildcard permission for a class: write access for all instance fields of a newly created instance (after NEW instruction).

                        // putfield: [..., objectRef, newValue] -> [...]

                        // Operand stack
                        // [..., objectRef, newValue]

                        int localVariableTableSlot = -1;
                        if (isPrimitiveLong(fieldType)) {
                            localVariableTableSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                            codeBuilder.lstore(localVariableTableSlot);
                        } else if (isPrimitiveDouble(fieldType)) {
                            localVariableTableSlot = codeBuilder.allocateLocal(TypeKind.DOUBLE);
                            codeBuilder.dstore(localVariableTableSlot);
                        } else {
                            codeBuilder.swap();
                        }
                        // [..., newValue, objectRef]
                        codeBuilder.dup();
                        // [..., newValue, objectRef, objectRef]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., newValue, objectRef, objectRef, declaringClass]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, objectRef, objectRef, declaringClass, fieldName]
                        writeAccess(codeBuilder);
                        // [..., newValue, objectRef, objectRef, declaringClass, fieldName, Access.WRITE]
                        invokeLogFieldAccess(codeBuilder);
                        // [..., newValue, objectRef]
                        if (isPrimitiveLong(fieldType)) {
                            codeBuilder.lload(localVariableTableSlot);
                        } else if (isPrimitiveDouble(fieldType)) {
                            codeBuilder.dload(localVariableTableSlot);
                        } else {
                            codeBuilder.swap();
                        }
                        // [..., objectRef, newValue]
                        codeBuilder.with(codeElement);
                        // [...]
                        break;
                    case Opcode.GETSTATIC:
                        // getstatic: [...] -> [..., fieldValue]

                        // Stack:
                        // [...]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., Owner.class]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., Owner.class, declaringClass]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., Owner.class, declaringClass, fieldName]
                        readAccess(codeBuilder);
                        // [..., Owner.class, declaringClass, fieldName, Access.READ]
                        invokeLogFieldAccess(codeBuilder);
                        // [...]
                        codeBuilder.with(codeElement);
                        // [..., fieldValue]
                        break;
                    case Opcode.PUTSTATIC:
                        // putstatic: [..., newValue] -> [...]

                        // Stack:
                        // [..., newValue]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., newValue, Owner.class]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., newValue, Owner.class, declaringClass]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, Owner.class, declaringClass, fieldName]
                        writeAccess(codeBuilder);
                        // [..., newValue, Owner.class, declaringClass, fieldName, Access.WRITE]
                        invokeLogFieldAccess(codeBuilder);
                        // [..., newValue]
                        codeBuilder.with(codeElement);
                        // [...]
                        break;
                }
            }

            else if (codeElement instanceof InvokeInstruction invokeInstruction
                    && invokeInstruction.owner().matches(CD_FIELD)) {

                if (invokeInstruction.name().equalsString("get") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getBoolean") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_BOOLEAN)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getByte") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_BYTE)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getChar") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_CHAR)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getDouble") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_DOUBLE)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getFloat") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_FLOAT)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getInt") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_INT)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getLong") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_LONG)) {
                    genMonitorFieldRead(codeBuilder);
                } else if (invokeInstruction.name().equalsString("getShort") && invokeInstruction.typeSymbol().equals(MTD_FIELD_GET_SHORT)) {
                    genMonitorFieldRead(codeBuilder);
                }

                else if (invokeInstruction.name().equalsString("set") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.REFERENCE);
                } else if (invokeInstruction.name().equalsString("setBoolean") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_BOOLEAN)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.BOOLEAN);
                } else if (invokeInstruction.name().equalsString("setByte") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_BYTE)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.BYTE);
                } else if (invokeInstruction.name().equalsString("setChar") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_CHAR)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.CHAR);
                } else if (invokeInstruction.name().equalsString("setDouble") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_DOUBLE)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.DOUBLE);
                } else if (invokeInstruction.name().equalsString("setFloat") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_FLOAT)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.FLOAT);
                } else if (invokeInstruction.name().equalsString("setInt") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_INT)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.INT);
                } else if (invokeInstruction.name().equalsString("setLong") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_LONG)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.LONG);
                } else if (invokeInstruction.name().equalsString("setShort") && invokeInstruction.typeSymbol().equals(MTD_FIELD_SET_SHORT)) {
                    genMonitorFieldWrite(codeBuilder, TypeKind.SHORT);
                }

                codeBuilder.with(codeElement);
            }

            else {
                // proceed with normal code
                codeBuilder.with(codeElement);
            }
        }));
    }

    private static boolean isPrimitiveLong(ClassDesc type) {
        return ConstantDescs.CD_long.equals(type);
    }

    private static boolean isPrimitiveDouble(ClassDesc type) {
        return ConstantDescs.CD_double.equals(type);
    }


    private static final ClassDesc CD_FIELD = ClassDesc.of("java.lang.reflect", "Field");
    private static final MethodTypeDesc
            MTD_FIELD_GET = getMethodDescriptor(CD_Object),
            MTD_FIELD_GET_BOOLEAN = getMethodDescriptor(CD_boolean),
            MTD_FIELD_GET_BYTE = getMethodDescriptor(CD_byte),
            MTD_FIELD_GET_CHAR = getMethodDescriptor(CD_char),
            MTD_FIELD_GET_DOUBLE = getMethodDescriptor(CD_double),
            MTD_FIELD_GET_FLOAT = getMethodDescriptor(CD_float),
            MTD_FIELD_GET_INT = getMethodDescriptor(CD_int),
            MTD_FIELD_GET_LONG = getMethodDescriptor(CD_long),
            MTD_FIELD_GET_SHORT = getMethodDescriptor(CD_short);
    private static final MethodTypeDesc
            MTD_FIELD_SET = setMethodDescriptor(CD_Object),
            MTD_FIELD_SET_BOOLEAN = setMethodDescriptor(CD_boolean),
            MTD_FIELD_SET_BYTE = setMethodDescriptor(CD_byte),
            MTD_FIELD_SET_CHAR = setMethodDescriptor(CD_char),
            MTD_FIELD_SET_DOUBLE = setMethodDescriptor(CD_double),
            MTD_FIELD_SET_FLOAT = setMethodDescriptor(CD_float),
            MTD_FIELD_SET_INT = setMethodDescriptor(CD_int),
            MTD_FIELD_SET_LONG = setMethodDescriptor(CD_long),
            MTD_FIELD_SET_SHORT = setMethodDescriptor(CD_short);

    private static MethodTypeDesc getMethodDescriptor(ClassDesc returnType) {
        return MethodTypeDesc.of(returnType, CD_Object);
    }

    private static MethodTypeDesc setMethodDescriptor(ClassDesc paramType) {
        return MethodTypeDesc.of(CD_void, CD_Object, paramType);
    }

    private static void genMonitorFieldRead(CodeBuilder codeBuilder) {
        // java.lang.reflect.Field#getX : [..., java.lang.reflect.Field, Object] -> [..., Object]

        // Operand stack:
        // [..., Field, objectInstance]
        int objectInstance = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.astore(objectInstance);
        // [..., Field]
        int reflectField = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.astore(reflectField);
        // [...]

        codeBuilder.aload(objectInstance);
        // [..., objectInstance]
        codeBuilder.ifThenElse(
                Opcode.IFNULL,
                thenBuilder -> {
                    // Trying to access a static field. the owningInstance is DeclaringClass.class
                    thenBuilder.aload(reflectField);
                    thenBuilder.invokevirtual(CD_FIELD, "getDeclaringClass", MethodTypeDesc.of(CD_Class));
                }, elseBuilder -> {
                    // Trying to access an instance field. the owningInstance is simply objectInstance
                    elseBuilder.aload(objectInstance);
                }
        );
        // [..., owningObject]
        codeBuilder.aload(reflectField);
        codeBuilder.invokevirtual(CD_FIELD, "getDeclaringClass", MethodTypeDesc.of(CD_Class));
        // [..., owningObject, declaringClass]
        codeBuilder.aload(reflectField);
        codeBuilder.invokevirtual(CD_FIELD, "getName", MethodTypeDesc.of(CD_String));
        // [..., owningObject, declaringClass, fieldName]
        readAccess(codeBuilder);
        // [..., owningObject, declaringClass, fieldName, Access.READ]
        invokeLogFieldAccess(codeBuilder);
        // [...]

        codeBuilder.aload(reflectField);
        codeBuilder.aload(objectInstance);
        // [..., java.lang.reflect.Field, objectInstance]
    }

    private static void genMonitorFieldWrite(CodeBuilder codeBuilder, TypeKind typeKind) {
        // java.lang.reflect.Field#setX : [..., java.lang.reflect.Field, Object, value] -> [...]

        // Operand stack:
        // [..., Field, objectInstance, value]
        int value = codeBuilder.allocateLocal(typeKind);
        codeBuilder.storeLocal(typeKind, value);
        // [..., Field, objectInstance]
        int objectInstance = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.astore(objectInstance);
        // [..., Field]
        int reflectField = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.astore(reflectField);
        // [...]

        codeBuilder.aload(objectInstance);
        // [..., objectInstance]
        codeBuilder.ifThenElse(
                Opcode.IFNULL,
                thenBuilder -> {
                    // Trying to access a static field. the owningInstance is DeclaringClass.class
                    thenBuilder.aload(reflectField);
                    thenBuilder.invokevirtual(CD_FIELD, "getDeclaringClass", MethodTypeDesc.of(CD_Class));
                }, elseBuilder -> {
                    // Trying to access an instance field. the owningInstance is simply objectInstance
                    elseBuilder.aload(objectInstance);
                }
        );
        // [..., owningObject]
        codeBuilder.aload(reflectField);
        codeBuilder.invokevirtual(CD_FIELD, "getDeclaringClass", MethodTypeDesc.of(CD_Class));
        // [..., owningObject, declaringClass]
        codeBuilder.aload(reflectField);
        codeBuilder.invokevirtual(CD_FIELD, "getName", MethodTypeDesc.of(CD_String));
        // [..., owningObject, declaringClass, fieldName]
        writeAccess(codeBuilder);
        // [..., owningObject, declaringClass, fieldName, Access.WRITE]
        invokeLogFieldAccess(codeBuilder);
        // [...]

        codeBuilder.aload(reflectField);
        codeBuilder.aload(objectInstance);
        codeBuilder.loadLocal(typeKind, value);
        // [..., java.lang.reflect.Field, objectInstance, value]
    }
}
