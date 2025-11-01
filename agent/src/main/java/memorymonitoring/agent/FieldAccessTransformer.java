package memorymonitoring.agent;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Opcode;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
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
import java.util.Optional;

public class FieldAccessTransformer implements ClassFileTransformer {

    private static final String RUNTIME_PACKAGE = "memorymonitoring.runtime";
    private static final ClassDesc MONITORED_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Monitored");
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

        ClassFile classFile = ClassFile.of();
        ClassModel classModel = classFile.parse(classfileBuffer);

        Optional<RuntimeInvisibleAnnotationsAttribute> optionalClassfileAnnotations = classModel.findAttribute(Attributes.runtimeInvisibleAnnotations());
        IO.println(optionalClassfileAnnotations); // TODO remove.
        if (optionalClassfileAnnotations.isPresent()) {
            RuntimeInvisibleAnnotationsAttribute annotationsAttribute = optionalClassfileAnnotations.get();

            for (Annotation annotation : annotationsAttribute.annotations()) {
                if (MONITORED_CLASSDESC.equals(annotation.classSymbol())) {
                    return classfileBuffer;
                }
            }
        }

        byte[] result = classFile.transformClass(classModel, ClassTransform.transformingMethodBodies((CodeBuilder codeBuilder, CodeElement codeElement) -> {
            if (codeElement instanceof FieldInstruction fieldInstruction) {
                FieldRefEntry fieldRefEntry = fieldInstruction.field();
                ClassEntry owningClass = fieldRefEntry.owner();
                Utf8Entry fieldName = fieldRefEntry.name();

                switch (fieldInstruction.opcode()) {
                    case Opcode.GETFIELD:
                        // getfield: [..., objectRef] -> [..., fieldValue]

                        // generate Permissions.logRead(new FieldReference(owningInstance, "theFieldName"));
                        codeBuilder.dup(); // put another copy of owningInstance on the operand stack
                        codeBuilder.ldc(fieldName.stringValue()); // put the field name as a string literal on the operand stack (note: String implements ConstantDesc)
                        // new FieldReference(<owningInstance>, <theFieldName>)
                        genNewFieldReference(codeBuilder);
                        // Permissions.logRead(<fieldReference>)
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logRead", LOG_METHOD_TYPE_DESC, false);
                        break;
                    case Opcode.PUTFIELD:
                        // putfield: [..., objectRef, newValue] -> [...]
                        codeBuilder.swap(); // set objectRef on top of the stack
                        codeBuilder.dup(); // put another copy of owningInstance on the operand stack
                        codeBuilder.ldc(fieldName.stringValue());
                        genNewFieldReference(codeBuilder);
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logWrite", LOG_METHOD_TYPE_DESC, false);
                        codeBuilder.swap(); // set the value to be written on top of the stack again.
                        break;
                    case Opcode.GETSTATIC:
                        // getstatic: [...] -> [..., fieldValue]
                        codeBuilder.ldc(owningClass.asSymbol());
                        codeBuilder.ldc(fieldName.stringValue());
                        genNewFieldReference(codeBuilder);
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logRead", LOG_METHOD_TYPE_DESC, false);
                        break;
                    case Opcode.PUTSTATIC:
                        // putstatic: [..., newValue] -> [...]
                        codeBuilder.ldc(owningClass.asSymbol());
                        codeBuilder.ldc(fieldName.stringValue());
                        genNewFieldReference(codeBuilder);
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logWrite", LOG_METHOD_TYPE_DESC, false);
                        break;
                }
            } // TODO array instructions? or should these be handled in a different class?

            // proceed with normal code (even if we encountered a field instruction)
            codeBuilder.with(codeElement);
        }));

        IO.println("TRANSFORMED CODE FOR CLASS: " + className);

        return result;
    }

    private static void genNewFieldReference(CodeBuilder codeBuilder) {
        codeBuilder.new_(FIELD_REFERENCE_CLASSDESC);
        codeBuilder.dup();
        codeBuilder.dup();
        codeBuilder.invokespecial(FIELD_REFERENCE_CLASSDESC, "<init>", FIELD_REFERENCE_CONSTRUCTOR_TYPE_DESC, false);
    }

}
