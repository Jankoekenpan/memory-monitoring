package memorymonitoring.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class References {

    private static final WeakHashMap<Object, Map<String, FieldReference>> fieldReferences = new WeakHashMap<>();
    private static final WeakHashMap<Object, Map<Integer, ArrayReference>> arrayReferences = new WeakHashMap<>();

    private References() {
    }

    public static synchronized FieldReference getFieldReference(Object owningInstance, String fieldName) {
        return getInnerMap(fieldReferences, owningInstance).computeIfAbsent(fieldName, _ -> new FieldReference(owningInstance, fieldName));
    }

    public static synchronized ArrayReference getArrayReference(Object arrayInstance, int index) {
        return getInnerMap(arrayReferences, arrayInstance).computeIfAbsent(index, _ -> new ArrayReference(arrayInstance, index));
    }

    private static <InnerKey, InnerValue> Map<InnerKey, InnerValue> getInnerMap(Map<Object, Map<InnerKey, InnerValue>> outer, Object outerKey) {
        return outer.computeIfAbsent(outerKey, _ -> new HashMap<>());
    }

}
