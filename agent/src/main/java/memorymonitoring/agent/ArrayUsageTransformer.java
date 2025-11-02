package memorymonitoring.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import static memorymonitoring.agent.RuntimeApiConstants.*;

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
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getArrayReference", GET_ARRAY_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., arr, index, ArrayReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logRead", LOG_METHOD_TYPE_DESC, false);
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
                        codeBuilder.invokestatic(REFERENCES_CLASSDESC, "getArrayReference", GET_ARRAY_REFERENCE_METHOD_TYPE_DESC, false);
                        // [..., arr, index, ArrayReference]
                        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logWrite", LOG_METHOD_TYPE_DESC, false);
                        // [..., arr, index]
                        codeBuilder.loadLocal(elementType, localVariableTableSlot);
                        // [..., arr, index, value]
                        codeBuilder.with(codeElement);
                        // [...]
                    }

                    else {
                        // proceed with normal code
                        codeBuilder.with(codeElement);
                    }
                }
        ));
    }
}
