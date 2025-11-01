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

public class FieldAccessTransformer implements ClassFileTransformer {

    private static final String RUNTIME_PACKAGE = "memorymonitoring.runtime";
    private static final ClassDesc FIELD_REFERENCE_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "FieldReference");
    private static final MethodTypeDesc FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_String);
    private static final ClassDesc PERMISSIONS_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Permissions");
    private static final MethodTypeDesc LOG_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, FIELD_REFERENCE_CLASSDESC);

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

                // TODO currently does not work for double and long (because they occupy two slots on the operand stack)
                // TODO from them we should use different dup and swap instructions.
                switch (fieldInstruction.opcode()) {
                    case Opcode.GETFIELD:
                        // getfield: [..., objectRef] -> [..., fieldValue]

                        // Operand stack:
                        // [..., objectRef]
                        codeBuilder.dup();
                        // [..., objectRef, objectRef]
                        codeBuilder.new_(FIELD_REFERENCE_CLASSDESC);
                        // [..., objectRef, objectRef, FieldReference]
                        codeBuilder.dup_x1();
                        // [..., objectRef, FieldReference, objectRef, FieldReference]
                        codeBuilder.swap();
                        // [..., objectRef, FieldReference, FieldReference, objectRef]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., objectRef, FieldReference, FieldReference, objectRef, fieldName]
                        codeBuilder.invokespecial(FIELD_REFERENCE_CLASSDESC, "<init>", FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC, false);
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
                        codeBuilder.new_(FIELD_REFERENCE_CLASSDESC);
                        // [..., newValue, objectRef, objectRef, FieldReference]
                        codeBuilder.dup_x1();
                        // [..., newValue, objectRef, FieldReference, objectRef, FieldReference]
                        codeBuilder.swap();
                        // [..., newValue, objectRef, FieldReference, FieldReference, objectRef]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, objectRef, FieldReference, FieldReference, objectRef, fieldName]
                        codeBuilder.invokespecial(FIELD_REFERENCE_CLASSDESC, "<init>", FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC, false);
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
                        codeBuilder.new_(FIELD_REFERENCE_CLASSDESC);
                        // [..., FieldReference]
                        codeBuilder.dup();
                        // [..., FieldReference, FieldReference]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., FieldReference, FieldReference, Owner.class]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., FieldReference, FieldReference, Owner.class, fieldName]
                        codeBuilder.invokespecial(FIELD_REFERENCE_CLASSDESC, "<init>", FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC, false);
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
                        codeBuilder.new_(FIELD_REFERENCE_CLASSDESC);
                        // [..., newValue, FieldReference]
                        codeBuilder.dup();
                        // [..., newValue, FieldReference, FieldReference]
                        codeBuilder.ldc(owningClass.asSymbol());
                        // [..., newValue, FieldReference, FieldReference, Owner.class]
                        codeBuilder.ldc(fieldName.stringValue());
                        // [..., newValue, FieldReference, FieldReference, Owner.class, fieldName]
                        codeBuilder.invokespecial(FIELD_REFERENCE_CLASSDESC, "<init>", FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC, false);
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
