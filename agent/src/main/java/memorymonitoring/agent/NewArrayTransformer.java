/*
 (C) COPYRIGHT 2025 TECHNOLUTION BV, GOUDA NL
 */
package memorymonitoring.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 *
 */
public final class NewArrayTransformer implements ClassFileTransformer {

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

        return classFile.transformClass(classModel, ClassTransform.transformingMethodBodies((CodeBuilder codeBuilder, CodeElement codeElement) -> {

            if (codeElement instanceof NewPrimitiveArrayInstruction newPrimitiveArrayInstruction) {
                codeBuilder.with(codeElement);
                // TODO grant write access to the whole array
            }

            else if (codeElement instanceof NewReferenceArrayInstruction newReferenceArrayInstruction) {
                codeBuilder.with(codeElement);
                // TODO grant write access to the whole array
            }

            else if (codeElement instanceof NewMultiArrayInstruction newMultiArrayInstruction) {
                codeBuilder.with(codeElement);
                // TODO grant write access to the whole array
            }

            else {
                // Leave all other instructions unchanged.
                codeBuilder.with(codeElement);
            }
        }));
    }

}