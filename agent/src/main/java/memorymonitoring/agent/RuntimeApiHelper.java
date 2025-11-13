package memorymonitoring.agent;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

final class RuntimeApiHelper {

    static final String RUNTIME_PACKAGE = "memorymonitoring.runtime";
    static final ClassDesc ACCESS_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Access");
    static final ClassDesc PERMISSIONS_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Permissions");
    static final ClassDesc REFLECT_FIELD_CLASSDESC = ClassDesc.of("java.lang.reflect", "Field");
    static final MethodTypeDesc SET_FIELD_PERMISSION_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_Class, ConstantDescs.CD_String, ACCESS_CLASSDESC);
    static final MethodTypeDesc SET_FIELD_DEFAULT_PERMISSION_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_Class, ConstantDescs.CD_String, ACCESS_CLASSDESC);
    static final MethodTypeDesc SET_ARRAY_PERMISSION_WHOLE_ARRAY_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ACCESS_CLASSDESC);
    static final MethodTypeDesc SET_ARRAY_PERMISSION_WHOLE_MULTI_ARRAY_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_int, ACCESS_CLASSDESC);
    static final MethodTypeDesc LOG_FIELD_ACCESS_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_Class, ConstantDescs.CD_String, ACCESS_CLASSDESC);
    static final MethodTypeDesc LOG_REFLECT_FIELD_ACCESS_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, REFLECT_FIELD_CLASSDESC, ConstantDescs.CD_Object, ACCESS_CLASSDESC);
    static final MethodTypeDesc LOG_ARRAY_ACCESS_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_int, ACCESS_CLASSDESC);
    static final MethodTypeDesc LOG_ARRAY_ACCESS_RANGE_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_int, ConstantDescs.CD_int, ACCESS_CLASSDESC);

    private RuntimeApiHelper() {
    }

    static void readAccess(CodeBuilder codeBuilder) {
        access(codeBuilder, "READ");
    }

    static void writeAccess(CodeBuilder codeBuilder) {
        access(codeBuilder, "WRITE");
    }

    private static void access(CodeBuilder codeBuilder, String accessEnumConstant) {
        codeBuilder.getstatic(ACCESS_CLASSDESC, accessEnumConstant, ACCESS_CLASSDESC);
    }

    static void invokeLogFieldAccess(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logFieldAccess", LOG_FIELD_ACCESS_METHOD_TYPE_DESC, false);
    }

    static void invokeLogReflectFieldAccess(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logFieldAccess", LOG_REFLECT_FIELD_ACCESS_METHOD_TYPE_DESC, false);
    }

    static void invokeLogArrayAccess(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logArrayAccess", LOG_ARRAY_ACCESS_METHOD_TYPE_DESC, false);
    }

    static void invokeLogArrayAccess_range(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "logArrayAccess", LOG_ARRAY_ACCESS_RANGE_METHOD_TYPE_DESC, false);
    }

    static void invokeSetFieldPermission(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "setFieldPermission", SET_FIELD_PERMISSION_METHOD_TYPE_DESC, false);
    }

    static void invokeSetArrayPermissionWholeArray(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "setArrayPermissionWholeArray", SET_ARRAY_PERMISSION_WHOLE_ARRAY_METHOD_TYPE_DESC, false);
    }

    static void invokeSetArrayPermissionWholeMultiArray(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "setArrayPermissionWholeMultiArray", SET_ARRAY_PERMISSION_WHOLE_MULTI_ARRAY_METHOD_TYPE_DESC, false);
    }

    static void invokeSetFieldDefaultPermission(CodeBuilder codeBuilder) {
        codeBuilder.invokestatic(PERMISSIONS_CLASSDESC, "setFieldDefaultPermission", SET_FIELD_DEFAULT_PERMISSION_METHOD_TYPE_DESC, false);
    }
}
