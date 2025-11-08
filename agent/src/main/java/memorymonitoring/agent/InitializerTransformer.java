package memorymonitoring.agent;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.AccessFlag;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static memorymonitoring.agent.RuntimeApiHelper.*;

final class InitializerTransformer implements ClassFileTransformer {

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

        List<FieldModel> fields = classModel.fields();
        ArrayList<FieldModel> instanceFields = new ArrayList<>(fields.size());
        ArrayList<FieldModel> staticFields = new ArrayList<>(fields.size());
        for (FieldModel field : fields) {
            (field.flags().has(AccessFlag.STATIC) ? staticFields : instanceFields).add(field);
        }
        instanceFields.trimToSize();
        staticFields.trimToSize();

        boolean hasClassInitializer = classModel.methods().stream().anyMatch(InitializerTransformer::isClassInitializer);
        ClassDesc thisClass = classModel.thisClass().asSymbol();
        Optional<ClassEntry> superClass = classModel.superclass();

        return classFile.transformClass(classModel, (ClassBuilder classBuilder, ClassElement classElement) -> {
            // Add class initialiser to the classs, if one is absent.
            if (!hasClassInitializer) {
                classBuilder.withMethodBody(
                        ConstantDescs.CLASS_INIT_NAME,
                        ConstantDescs.MTD_void,
                        AccessFlag.PUBLIC.mask() | AccessFlag.STATIC.mask(),
                        (CodeBuilder codeBuilder) -> instrumentStaticInitializer(codeBuilder, thisClass, staticFields)
                );
            } // otherwise, static initialiser will be instrumented.

            if (classElement instanceof MethodModel maybeConstructorMethod && isConstructor(maybeConstructorMethod)) {
                MethodTypeDesc methodTypeDescriptor = maybeConstructorMethod.methodTypeSymbol();
                int flags = maybeConstructorMethod.flags().flagsMask();
                Optional<CodeModel> code = maybeConstructorMethod.code();
                CodeModel codeModel = code.get();

                classBuilder.withMethod(ConstantDescs.INIT_NAME, methodTypeDescriptor, flags, methodBuilder -> methodBuilder.transformCode(codeModel, new CodeTransform() {
                    @Override
                    public void accept(CodeBuilder codeBuilder, CodeElement codeElement) {
                        if (codeElement instanceof InvokeInstruction instruction && isInvokeSuperConstructor(superClass, instruction)) {
                            codeBuilder.with(instruction); // invoke super constructor

                            // Grant write permission (has to occur after super constructor call).
                            for (FieldModel instanceFieldModel : instanceFields) {
                                // [...]
                                codeBuilder.aload(0);
                                // [..., this]
                                codeBuilder.ldc(instanceFieldModel.fieldName().stringValue());
                                // [..., this, "someField"]
                                writeAccess(codeBuilder);
                                // [..., this, "someField", Access.WRITE]
                                invokeSetFieldPermission(codeBuilder);
                                // [...]
                            }
                        } else {
                            codeBuilder.with(codeElement);
                        }
                    }
                }));

            }

            else if (hasClassInitializer && classElement instanceof MethodModel maybeClassInitializerMethod && isClassInitializer(maybeClassInitializerMethod)) {
                int flags = maybeClassInitializerMethod.flags().flagsMask();
                Optional<CodeModel> code = maybeClassInitializerMethod.code();
                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, ConstantDescs.MTD_void, flags, (CodeBuilder codeBuilder) -> {
                    if (code.isPresent()) {
                        CodeModel codeModel = code.get();
                        // Set Access.WRITE permission for all static fields.
                        // Note, the generated code does not contain an extra loop; the field permissions are just written inlined/unrolled.

                        instrumentStaticInitializer(codeBuilder, thisClass, staticFields);

                        // Write the original class initializer instructions afterwards.
                        codeModel.forEach(codeBuilder::with);
                    }
                });
            }

            else {
                // Not a constructor or class initializer
                classBuilder.accept(classElement);
            }
        });
    }

    private static boolean isInvokeSuperConstructor(Optional<ClassEntry> superClass, InvokeInstruction invokeInstruction) {
        if (superClass.isEmpty()) return false;

        return invokeInstruction.opcode() == Opcode.INVOKESPECIAL
                && !invokeInstruction.isInterface()
                && invokeInstruction.name().equalsString(ConstantDescs.INIT_NAME)
                && invokeInstruction.owner().equals(superClass.get())
                && invokeInstruction.typeSymbol().returnType().equals(ConstantDescs.CD_void);
    }

    private static void instrumentStaticInitializer(CodeBuilder codeBuilder, ClassDesc thisClass, List<FieldModel> staticFields) {
        // [...]
        for (FieldModel staticFieldModel : staticFields) {
            // [...]
            codeBuilder.ldc(thisClass);
            // [..., Owner.class]
            codeBuilder.ldc(staticFieldModel.fieldName().stringValue());
            // [..., Owner.class, "someField"]
            writeAccess(codeBuilder);
            // [..., Owner.class, "someField", Access.WRITE]
            invokeSetFieldPermission(codeBuilder);
            // [...]
        }
        // [...]
    }

    private static boolean isConstructor(MethodModel methodModel) {
        return methodModel.methodName().equalsString(ConstantDescs.INIT_NAME)
                && !methodModel.flags().has(AccessFlag.STATIC) && !methodModel.flags().has(AccessFlag.ABSTRACT)
                && methodModel.methodTypeSymbol().returnType().equals(ConstantDescs.CD_void);
    }

    private static boolean isClassInitializer(MethodModel methodModel) {
        return methodModel.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME)
                && methodModel.flags().has(AccessFlag.STATIC)
                && methodModel.methodTypeSymbol().equals(ConstantDescs.MTD_void);
    }
}
