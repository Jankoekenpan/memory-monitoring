package memorymonitoring.agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import static memorymonitoring.agent.RuntimeApiHelper.*;

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
                        readAccess(codeBuilder);
                        // [..., arr, index, arr, index, Access.READ]
                        invokeLogArrayAccess(codeBuilder);
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
                        writeAccess(codeBuilder);
                        // [..., arr, index, arr, index, Access.WRITE]
                        invokeLogArrayAccess(codeBuilder);
                        // [..., arr, index]
                        codeBuilder.loadLocal(elementType, localVariableTableSlot);
                        // [..., arr, index, value]
                        codeBuilder.with(codeElement);
                        // [...]
                    }

                    else if (codeElement instanceof InvokeInstruction invokeInstruction && isInvokeSystemArrayCopy(invokeInstruction)) {
                        // invokestatic: [..., arg1, ..., argN] -> [..., [resultValue]]

                        // Operand stack:
                        // [..., srcArr, srcPos, destArr, destPos, length]
                        int length = codeBuilder.allocateLocal(TypeKind.INT);
                        codeBuilder.istore(length);
                        int destPos = codeBuilder.allocateLocal(TypeKind.INT);
                        codeBuilder.istore(destPos);
                        int destArr = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                        codeBuilder.astore(destArr);
                        int srcPos = codeBuilder.allocateLocal(TypeKind.INT);
                        codeBuilder.istore(srcPos);
                        int srcArr = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                        codeBuilder.astore(srcArr);
                        // [...]

                        codeBuilder.aload(srcArr);
                        // [..., srcArr]
                        codeBuilder.iload(srcPos);
                        // [..., srcArr, srcPos]
                        codeBuilder.dup();
                        // [..., srcArr, srcPos, srcPos]
                        codeBuilder.iload(length);
                        // [..., srcArr, srcPos, srcPos, length];
                        codeBuilder.iadd();
                        // [..., srcArr, srcPos, indexTo];
                        readAccess(codeBuilder);
                        // [..., srcArr, srcPos, indexTo, Access.READ];
                        invokeLogArrayAccess_range(codeBuilder);
                        // [...]

                        codeBuilder.aload(destArr);
                        // [..., destArr]
                        codeBuilder.iload(destPos);
                        // [..., destArr, destPos]
                        codeBuilder.dup();
                        // [..., destArr, destPos, destPos]
                        codeBuilder.iload(length);
                        // [..., destArr, destPos, destPos, length]
                        codeBuilder.iadd();
                        // [..., destArr, destPos, indexTo]
                        writeAccess(codeBuilder);
                        // [..., destArr, destPos, indexTo, Access.WRITE]
                        invokeLogArrayAccess_range(codeBuilder);
                        // [...]

                        codeBuilder.aload(srcArr);
                        codeBuilder.iload(srcPos);
                        codeBuilder.aload(destArr);
                        codeBuilder.iload(destPos);
                        codeBuilder.iload(length);

                        // [..., srcArr, srcPos, destArr, destPos, length]
                        codeBuilder.with(codeElement);
                        // [...]
                    }

                    // TODO intercept calls to java.lang.reflect.Array methods
                    // TODO     check read permission for Array.get and Array.getXXX (where XXX is a primitive type)
                    // TODO     check write permission for Array.set and Array.setXXX (where XXX is a primitive type)

                    else {
                        // proceed with normal code
                        codeBuilder.with(codeElement);
                    }
                }
        ));
    }

    private static boolean isInvokeSystemArrayCopy(InvokeInstruction invokeInstruction) {
        return invokeInstruction.owner().matches(CD_SYSTEM)
                && !invokeInstruction.isInterface()
                && invokeInstruction.name().equalsString("arrayCopy")
                && invokeInstruction.typeSymbol().equals(MTD_System_arrayCopy);
    }

    private static final ClassDesc CD_SYSTEM = ClassDesc.of("java.lang", "System");
    private static final MethodTypeDesc MTD_System_arrayCopy = MethodTypeDesc.of(ConstantDescs.CD_void,
            ConstantDescs.CD_Object,
            ConstantDescs.CD_int,
            ConstantDescs.CD_Object,
            ConstantDescs.CD_int,
            ConstantDescs.CD_int);
}
