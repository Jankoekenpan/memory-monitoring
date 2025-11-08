package memorymonitoring.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import static java.lang.constant.ConstantDescs.*;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import static memorymonitoring.agent.RuntimeApiHelper.*;

final class ArrayUsageTransformer implements ClassFileTransformer {

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

                    if (codeElement instanceof ArrayLoadInstruction arrayLoadInstruction) {
                        // aXload: [..., arr, index] -> [..., element]

                        // Stack:
                        // [..., arr, index]
                        codeBuilder.dup2();
                        // [..., arr, index, arr, index]
                        readAccess(codeBuilder);
                        // [..., arr, index, arr, index, Access.READ]
                        invokeLogArrayAccess(codeBuilder);
                        // [..., arr, index]
                        codeBuilder.with(codeElement);
                        // [..., element]
                    }

                    else if (codeElement instanceof ArrayStoreInstruction arrayStoreInstruction) {
                        // aXstore: [..., arr, index, value] -> [...]
                        TypeKind elementType = arrayStoreInstruction.typeKind();

                        // Stack:
                        // [..., arr, index, value]
                        int localVariableTableSlot = codeBuilder.allocateLocal(elementType);
                        codeBuilder.storeLocal(elementType, localVariableTableSlot);
                        // [..., arr, index]
                        codeBuilder.dup2();
                        // [..., arr, index, arr, index]
                        writeAccess(codeBuilder);
                        // [..., arr, index, arr, index, Access.WRITE]
                        invokeLogArrayAccess(codeBuilder);
                        // [..., arr, index]
                        codeBuilder.loadLocal(elementType, localVariableTableSlot);
                        // [..., arr, index, value]
                        codeBuilder.with(codeElement);
                        // [...]
                    }

                    else if (codeElement instanceof InvokeInstruction invokeInstruction && isInvokeSystemArrayCopy(invokeInstruction)) {
                        // invokestatic: [..., arg1, ..., argN] -> [..., [resultValue]]

                        // Operand stack:
                        // [..., srcArr, srcPos, destArr, destPos, length]
                        int length = codeBuilder.allocateLocal(TypeKind.INT);
                        codeBuilder.istore(length);
                        int destPos = codeBuilder.allocateLocal(TypeKind.INT);
                        codeBuilder.istore(destPos);
                        int destArr = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                        codeBuilder.astore(destArr);
                        int srcPos = codeBuilder.allocateLocal(TypeKind.INT);
                        codeBuilder.istore(srcPos);
                        int srcArr = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                        codeBuilder.astore(srcArr);
                        // [...]

                        codeBuilder.aload(srcArr);
                        // [..., srcArr]
                        codeBuilder.iload(srcPos);
                        // [..., srcArr, srcPos]
                        codeBuilder.dup();
                        // [..., srcArr, srcPos, srcPos]
                        codeBuilder.iload(length);
                        // [..., srcArr, srcPos, srcPos, length];
                        codeBuilder.iadd();
                        // [..., srcArr, srcPos, indexTo];
                        readAccess(codeBuilder);
                        // [..., srcArr, srcPos, indexTo, Access.READ];
                        invokeLogArrayAccess_range(codeBuilder);
                        // [...]

                        codeBuilder.aload(destArr);
                        // [..., destArr]
                        codeBuilder.iload(destPos);
                        // [..., destArr, destPos]
                        codeBuilder.dup();
                        // [..., destArr, destPos, destPos]
                        codeBuilder.iload(length);
                        // [..., destArr, destPos, destPos, length]
                        codeBuilder.iadd();
                        // [..., destArr, destPos, indexTo]
                        writeAccess(codeBuilder);
                        // [..., destArr, destPos, indexTo, Access.WRITE]
                        invokeLogArrayAccess_range(codeBuilder);
                        // [...]

                        codeBuilder.aload(srcArr);
                        codeBuilder.iload(srcPos);
                        codeBuilder.aload(destArr);
                        codeBuilder.iload(destPos);
                        codeBuilder.iload(length);

                        // [..., srcArr, srcPos, destArr, destPos, length]
                        codeBuilder.with(codeElement);
                        // [...]
                    }

                    else if (codeElement instanceof InvokeInstruction invokeInstruction
                            && invokeInstruction.owner().matches(CD_ARRAY)
                            && !invokeInstruction.isInterface()
                            && invokeInstruction.opcode() == Opcode.INVOKESTATIC) {
                        handleJavaLangReflectArrayInvocation(codeBuilder, invokeInstruction);
                        codeBuilder.with(codeElement);
                    }

                    else {
                        // proceed with normal code
                        codeBuilder.with(codeElement);
                    }
                }
        ));
    }

    private static boolean isInvokeSystemArrayCopy(InvokeInstruction invokeInstruction) {
        return invokeInstruction.owner().matches(CD_SYSTEM)
                && !invokeInstruction.isInterface()
                && invokeInstruction.name().equalsString("arrayCopy")
                && invokeInstruction.typeSymbol().equals(MTD_System_arrayCopy);
    }

    private static final ClassDesc CD_SYSTEM = ClassDesc.of("java.lang", "System");
    private static final MethodTypeDesc MTD_System_arrayCopy = MethodTypeDesc.of(CD_void,
            CD_Object, CD_int, CD_Object, CD_int, CD_int);

    private static final ClassDesc CD_ARRAY = ClassDesc.of("java.lang.reflect", "Array");

    private static void handleJavaLangReflectArrayInvocation(CodeBuilder codeBuilder, InvokeInstruction invokeInstruction) {
        if (invokeInstruction.name().equalsString("get") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getBoolean") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_BOOLEAN)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getByte") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_BYTE)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getChar") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_CHAR)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getDouble") && invokeInstruction.typeSymbol().equals(MTD_ARAY_GET_DOUBLE)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getFloat") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_FLOAT)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getInt") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_INT)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getLong") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_LONG)) {
            genGetReadAccess(codeBuilder);
        } else if (invokeInstruction.name().equalsString("getShort") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_GET_SHORT)) {
            genGetReadAccess(codeBuilder);
        }

        else if (invokeInstruction.name().equalsString("set") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET)) {
            genGetWriteAccess(codeBuilder, TypeKind.REFERENCE);
        } else if (invokeInstruction.name().equalsString("setBoolean") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_BOOLEAN)) {
            genGetWriteAccess(codeBuilder, TypeKind.BOOLEAN);
        } else if (invokeInstruction.name().equalsString("setByte") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_BYTE)) {
            genGetWriteAccess(codeBuilder, TypeKind.BYTE);
        } else if (invokeInstruction.name().equalsString("setChar") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_CHAR)) {
            genGetWriteAccess(codeBuilder, TypeKind.CHAR);
        } else if (invokeInstruction.name().equalsString("setDouble") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_DOUBLE)) {
            genGetWriteAccess(codeBuilder, TypeKind.DOUBLE);
        } else if (invokeInstruction.name().equalsString("setFloat") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_FLOAT)) {
            genGetWriteAccess(codeBuilder, TypeKind.FLOAT);
        } else if (invokeInstruction.name().equalsString("setInt") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_INT)) {
            genGetWriteAccess(codeBuilder, TypeKind.INT);
        } else if (invokeInstruction.name().equalsString("setLong") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_LONG)) {
            genGetWriteAccess(codeBuilder, TypeKind.LONG);
        } else if (invokeInstruction.name().equalsString("setShort") && invokeInstruction.typeSymbol().equals(MTD_ARRAY_SET_SHORT)) {
            genGetWriteAccess(codeBuilder, TypeKind.SHORT);
        }
    }

    private static final MethodTypeDesc
            MTD_ARRAY_GET = getMethodDescriptor(CD_Object),
            MTD_ARRAY_GET_BOOLEAN = getMethodDescriptor(CD_boolean),
            MTD_ARRAY_GET_BYTE = getMethodDescriptor(CD_byte),
            MTD_ARRAY_GET_CHAR = getMethodDescriptor(CD_char),
            MTD_ARAY_GET_DOUBLE = getMethodDescriptor(CD_double),
            MTD_ARRAY_GET_FLOAT = getMethodDescriptor(CD_float),
            MTD_ARRAY_GET_INT = getMethodDescriptor(CD_int),
            MTD_ARRAY_GET_LONG = getMethodDescriptor(CD_long),
            MTD_ARRAY_GET_SHORT = getMethodDescriptor(CD_short);

    private static MethodTypeDesc getMethodDescriptor(ClassDesc returnType) {
        return MethodTypeDesc.of(returnType, CD_Object, CD_int);
    }

    private static void genGetReadAccess(CodeBuilder codeBuilder) {
        // [..., arr, index]
        codeBuilder.dup2();
        // [..., arr, index, arr, index]
        readAccess(codeBuilder);
        // [..., arr, index, arr, index, Access.READ]
        invokeLogArrayAccess(codeBuilder);
        // [..., arr, index]
    }

    private static final MethodTypeDesc
            MTD_ARRAY_SET = setMethodDescriptor(CD_Object),
            MTD_ARRAY_SET_BOOLEAN = setMethodDescriptor(CD_boolean),
            MTD_ARRAY_SET_BYTE = setMethodDescriptor(CD_byte),
            MTD_ARRAY_SET_CHAR = setMethodDescriptor(CD_char),
            MTD_ARRAY_SET_DOUBLE = setMethodDescriptor(CD_double),
            MTD_ARRAY_SET_FLOAT = setMethodDescriptor(CD_float),
            MTD_ARRAY_SET_INT = setMethodDescriptor(CD_int),
            MTD_ARRAY_SET_LONG = setMethodDescriptor(CD_long),
            MTD_ARRAY_SET_SHORT = setMethodDescriptor(CD_short);

    private static MethodTypeDesc setMethodDescriptor(ClassDesc valueType) {
        return MethodTypeDesc.of(CD_Object, CD_int, valueType);
    }

    private static void genGetWriteAccess(CodeBuilder codeBuilder, TypeKind typeKind) {
        // [..., arr, index, value]
        int localVariableTableIndex = codeBuilder.allocateLocal(typeKind);
        codeBuilder.storeLocal(typeKind, localVariableTableIndex);
        // [..., arr, index]
        codeBuilder.dup2();
        // [..., arr, index, arr, index]
        writeAccess(codeBuilder);
        // [..., arr, index, arr, index, Access.WRITE]
        invokeLogArrayAccess(codeBuilder);
        // [..., arr, index]
        codeBuilder.loadLocal(typeKind, localVariableTableIndex);
        // [..., arr, index, value]
    }
}
