package memorymonitoring.agent;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

final class RuntimeApiConstants {

    static final String RUNTIME_PACKAGE = "memorymonitoring.runtime";
    static final ClassDesc REFERENCE_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Reference");
    static final ClassDesc FIELD_REFERENCE_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "FieldReference");
    static final ClassDesc AARRAY_REFERENCE_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "ArrayReference");
    static final ClassDesc PERMISSIONS_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "Permissions");
    static final MethodTypeDesc LOG_METHOD_TYPE_DESC = MethodTypeDesc.of(ConstantDescs.CD_void, REFERENCE_CLASSDESC);
    static final ClassDesc REFERENCES_CLASSDESC = ClassDesc.of(RUNTIME_PACKAGE, "References");
    static final MethodTypeDesc GET_FIELD_REFERENCE_METHOD_TYPE_DESC = MethodTypeDesc.of(FIELD_REFERENCE_CLASSDESC, ConstantDescs.CD_Object, ConstantDescs.CD_String);
    static final MethodTypeDesc GET_ARRAY_REFERENCE_METHOD_TYPE_DESC = MethodTypeDesc.of(AARRAY_REFERENCE_CLASSDESC, ConstantDescs.CD_Object, ConstantDescs.CD_int);

    private RuntimeApiConstants() {
    }
}
