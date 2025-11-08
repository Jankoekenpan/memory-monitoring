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

                // Operand stack:
                // [..., count1 ..., countN]
                codeBuilder.with(codeElement);
                // [..., arr]
                codeBuilder.dup();
                // [..., arr, arr]
                writeAccess(codeBuilder);
                // [..., arr, arr, Access.WRITE]
                invokeSetArrayPermissionWholeArray(codeBuilder);
                // [..., arr]
            }

            else {
                // Leave all other instructions unchanged.
                codeBuilder.with(codeElement);
            }
        }));
    }

    // TODO can we also support System.arrayCopy? probably yes, once we have array range support.
    // TODO what about java.util.Arrays helpers? probably also yes for the copyOf methods. (range read access)

    // TODO should we care about passing array references to methods in general?
    // TODO perhaps we can track invokeInstructions and determine whether an array instance was passed to the called method.
    // TODO we should require write permission to the whole array, in the general case (we can special-case some JDK methods for read-only access)

    // TODO we can also take the philosophy that those library methods should also be instrumented by our agent, and *only* special-case System.arrayCopy.
    // TODO I like this last idea the best.
}