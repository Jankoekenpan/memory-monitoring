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

import static memorymonitoring.agent.RuntimeApiHelper.invokeSetArrayPermissionWholeArray;
import static memorymonitoring.agent.RuntimeApiHelper.invokeSetArrayPermissionWholeMultiArray;
import static memorymonitoring.agent.RuntimeApiHelper.writeAccess;

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
                // newarray: [..., count] -> [..., arr]

                // Operand stack:
                // [..., count]
                codeBuilder.with(codeElement);
                // [..., arr]
                codeBuilder.dup();
                // [..., arr, arr]
                writeAccess(codeBuilder);
                // [..., arr, arr, Access.WRITE]
                invokeSetArrayPermissionWholeArray(codeBuilder);
                // [..., arr]
            }

            else if (codeElement instanceof NewReferenceArrayInstruction newReferenceArrayInstruction) {
                // anewarray: [..., count] -> [..., arr]

                // Operand stack:
                // [..., count]
                codeBuilder.with(codeElement);
                // [..., arr]
                codeBuilder.dup();
                // [..., arr, arr]
                writeAccess(codeBuilder);
                // [..., arr, arr, Access.WRITE]
                invokeSetArrayPermissionWholeArray(codeBuilder);
                // [..., arr]
            }

            else if (codeElement instanceof NewMultiArrayInstruction newMultiArrayInstruction) {
                // multianewarray: [..., count1, ..., countN] -> [..., arr]

                int dimensions = newMultiArrayInstruction.dimensions();

                // Operand stack:
                // [..., count1 ..., countN]
                codeBuilder.with(codeElement);
                // [..., arr]
                codeBuilder.dup();
                // [..., arr, arr]
                codeBuilder.loadConstant(dimensions);
                // [..., arr, arr, dimensions]
                writeAccess(codeBuilder);
                // [..., arr, arr, dimensions, Access.WRITE]
                invokeSetArrayPermissionWholeMultiArray(codeBuilder);
                // [..., arr]
            }

            // TODO intercept calls to Array.newInstance
            // TODO determine which overload by checking the method parameter description. (cannot have VarArgs for arrays in Java.)

            else {
                // Leave all other instructions unchanged.
                codeBuilder.with(codeElement);
            }
        }));
    }
}