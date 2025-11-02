package memorymonitoring.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class FieldUsageTransformer implements ClassFileTransformer {

    private static final String RUNTIME_PACKAGE = "memorymonitoring.runtime";
    private static final ClassDesc FIELD_REFERENCE_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "FieldReference");
    private static final MethodTypeDesc FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_String);
    private static final ClassDesc PERMISSIONS_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Permissions");
    private static final MethodTypeDesc LOG_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, FIELD_REFERENCE_CLASSDESC);
    private static final ClassDesc REFERENCES_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "References");
    private static final MethodTypeDesc GET_FIELD_REFERENCE_METHOD_TYPE_DESC = MethodTypeDesc.of(FIELD_REFERENCE_CLASSDESC, ConstantDescs.CD_Object, ConstantDescs.CD_String);

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

        byte[] result = classFile.transformClass(classModel, ClassTransform.transformingMethodBodies(
                (CodeBuilder codeBuilder, CodeElement codeElement) -> {
            if (codeElement instanceof FieldInstruction fieldInstruction) {
                FieldRefEntry fieldRefEntry = fieldInstruction.field();
                ClassEntry owningClass = fieldRefEntry.owner();
                Utf8Entry fieldName = fieldRefEntry.name();
                ClassDesc fieldType = fieldRefEntry.typeSymbol(); // TODO special handling for double & long.

                // TODO currently does not work for double and long (because they occupy two slots on the operand stack)
                // TODO from them we should use different dup and swap instructions.
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
                        codeBuilder.swap();
                        // [..., newValue, objectRef]
                        codeBuilder.dup();
                        // [..., newValue, objectRef, objectRef]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, objectRef, objectRef, fieldName]
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getFieldReference", GET_FIELD_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., newValue, objectRef, FieldReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logWrite", LOG_METHOD_TYPE_DESC, false);
                        // [..., newValue, objectRef]
                        codeBuilder.swap();
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
            } // TODO array instructions? or should these be handled in a different class?

            else {
                // proceed with normal code
                codeBuilder.with(codeElement);
            }
        }));

        return result;
    }

}
