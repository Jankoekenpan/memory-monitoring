package memorymonitoring.agent;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
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

        return classFile.transformClass(classModel, (ClassBuilder classBuilder, ClassElement classElement) -> {

            if (classElement instanceof MethodModel maybeConstructorMethod && isConstructor(maybeConstructorMethod)) {
                MethodTypeDesc methodTypeDescriptor = maybeConstructorMethod.methodTypeSymbol();
                int flags = maybeConstructorMethod.flags().flagsMask();
                Optional<CodeModel> code = maybeConstructorMethod.code();
                classBuilder.withMethodBody(ConstantDescs.INIT_NAME, methodTypeDescriptor, flags, codeBuilder -> {
                    if (code.isPresent()) {
                        CodeModel codeModel = code.get();
                        // Set Access.WRITE permission for all instance fields.
                        // Note, the generated code does not contain an extra loop; the field permissions are just written inlined/unrolled.

                        // [...]
                        for (FieldModel instanceFieldModel : instanceFields) {
                            // [...]
                            codeBuilder.aload(0);
                            // [..., this]
                            codeBuilder.ldc(instanceFieldModel.fieldName().stringValue());
                            // [..., this, "someField"]
                            writeAccess(codeBuilder);
                            // [..., this, "someField, Access.WRITE]
                            invokeSetFieldPermission(codeBuilder);
                            // [...]
                        }
                        // [...]

                        // Write the original constructor instructions afterwards.
                        codeModel.forEach(codeBuilder::with);
                    }
                });
            }

            else if (classElement instanceof MethodModel maybeClassInitializerMethod && isClassInitializer(maybeClassInitializerMethod)) {
                int flags = maybeClassInitializerMethod.flags().flagsMask();
                Optional<CodeModel> code = maybeClassInitializerMethod.code();
                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, ConstantDescs.MTD_void, flags, codeBuilder -> {
                    if (code.isPresent()) {
                        CodeModel codeModel = code.get();
                        // Set Access.WRITE permission for all static fields.
                        // Note, the generated code does not contain an extra loop; the field permissions are just written inlined/unrolled.

                        // [...]
                        for (FieldModel staticFieldModel : staticFields) {
                            // [...]
                            codeBuilder.ldc(classModel.thisClass().asSymbol());
                            // [..., Owner.class]
                            codeBuilder.ldc(staticFieldModel.fieldName().stringValue());
                            // [..., Owner.class, "someField"]
                            writeAccess(codeBuilder);
                            // [..., Owner.class, "someField, Access.WRITE]
                            invokeSetFieldPermission(codeBuilder);
                            // [...]
                        }
                        codeBuilder.pop();
                        // [...]

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

    private static boolean isConstructor(MethodModel methodModel) {
        return methodModel.methodName().equalsString(ConstantDescs.INIT_NAME)
                && !methodModel.flags().has(AccessFlag.STATIC)
                && methodModel.methodTypeSymbol().returnType().equals(ConstantDescs.CD_void);
    }

    private static boolean isClassInitializer(MethodModel methodModel) {
        return methodModel.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME)
                && methodModel.flags().has(AccessFlag.STATIC)
                && methodModel.methodTypeSymbol().equals(ConstantDescs.MTD_void);
    }
}
