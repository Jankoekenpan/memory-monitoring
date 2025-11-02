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
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import static memorymonitoring.agent.RuntimeApiConstants.*;

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
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., objectRef, objectRef, fieldName]
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getFieldReference", GET_FIELD_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., objectRef, FieldReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logRead", LOG_METHOD_TYPE_DESC, false);
                        // [..., objectRef]
                        codeBuilder.with(codeElement);
                        // [..., fieldValue]
                        break;
                    case Opcode.PUTFIELD:
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
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, objectRef, objectRef, fieldName]
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getFieldReference", GET_FIELD_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., newValue, objectRef, FieldReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logWrite", LOG_METHOD_TYPE_DESC, false);
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
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., Owner.class, fieldName]
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getFieldReference", GET_FIELD_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., FieldReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logRead", LOG_METHOD_TYPE_DESC, false);
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
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, Owner.class, fieldName]
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getFieldReference", GET_FIELD_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., newValue, FieldReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logWrite", LOG_METHOD_TYPE_DESC, false);
                        // [..., newValue]
                        codeBuilder.with(codeElement);
                        // [...]
                        break;
                }
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
}
