package memorymonitoring.runtime;

import memorymonitoring.util.CalledByInstrumentedCode;
import memorymonitoring.util.SegmentTree;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Permissions {

    private static final Logger LOGGER = Logger.getLogger(Permissions.class.getName());

    // TODO could make use of Collections.synchronizedMap, if we can be sure that this won't cause deadlocks.
    // TODO i.e. while being locked on one of the instances, the other instance should never be accessed.
    // TODO I think our access patterns (and access patterns of generated code) is like this, so we could do this refactor (and remove a bunch of synchronized modifiers from methods in this class).
    private static final WeakHashMap<Object, WeakHashMap<Thread, Map<String, Access>>> fieldPermissions = new WeakHashMap<>();  // For static fields, the owning Object is an instance of java.lang.Class.
    private static final WeakHashMap<Object, WeakHashMap<Thread, SegmentTree<Access>>> arrayPermissions = new WeakHashMap<>();

    private Permissions() {}

    @CalledByInstrumentedCode
    public static void setFieldPermission(Object owningInstance, String fieldName, Access access) {
        setFieldPermission(Thread.currentThread(), owningInstance, fieldName, access);
    }

    public static synchronized void setFieldPermission(Thread thread, Object owningInstance, String fieldName, Access access) {
        String message = String.format("Giving %s permission to thread %s at object field %s.%s", access, thread.getName(), owningInstance, fieldName);
        LOGGER.info(message);

        // TODO check whether other threads have conflicting permission to the object field?

        fieldPermissions
                .computeIfAbsent(Objects.requireNonNull(owningInstance), _ -> new WeakHashMap<>())
                .computeIfAbsent(thread, _ -> new HashMap<>())
                .put(fieldName, access); // TODO when upgrading permission, log warning?
    }

    // not called by instrumented code (yet).
    public static void setArrayPermission(Object arrayInstance, int index, Access access) {
        setArrayPermission(arrayInstance, index, index + 1, access);
    }

    // not called by instrumented code (yet).
    public static void setArrayPermission(Object arrayInstance, int indexFrom, int indexTo, Access access) {
        setArrayPermission(Thread.currentThread(), arrayInstance, indexFrom, indexTo, access);
    }

    @CalledByInstrumentedCode
    public static void setArrayPermissionWholeArray(Object arrayInstance, Access access) {
        setArrayPermission(Thread.currentThread(), arrayInstance, 0, Array.getLength(arrayInstance), access);
    }

    @CalledByInstrumentedCode
    public static void setArrayPermissionWholeMultiArray(Object multiArray, int dimensions, Access access) {
        setArrayPermissionWholeArray(multiArray, access);

        if (dimensions > 1) {
            int length = Array.getLength(multiArray);
            for (int idx = 0; idx < length; idx += 1) {
                Object nestedArray = Array.get(multiArray, idx);
                setArrayPermissionWholeMultiArray(nestedArray, dimensions - 1, access);
            }
        }
    }

    /**
     * Set permission for the current thread to a range in the array
     * @param arrayInstance the array
     * @param indexFrom starting index - inclusive
     * @param indexTo end index - exclusive
     * @param access the permission level to be set
     */
    public static synchronized void setArrayPermission(Thread thread, Object arrayInstance, int indexFrom, int indexTo, Access access) {
        String message = String.format("Giving %s permission to thread %s at array range %s[%d, %d)", access, thread.getName(), arrayInstanceToString(arrayInstance), indexFrom, indexTo);
        LOGGER.info(message);

        // TODO check whether other threads have conflicting permission in this array range?

        arrayPermissions
                .computeIfAbsent(arrayInstance, _ -> new WeakHashMap<>())
                .computeIfAbsent(thread, _ -> new SegmentTree<>(Array.getLength(arrayInstance), Access.NONE, Access::weakest))
                .set(indexFrom, indexTo, access); // TODO chen upgrading permission, log warning?
    }

    public static synchronized Access getFieldPermission(Thread thread, Object owningInstance, String fieldName) {
        return Optional.ofNullable(fieldPermissions.get(owningInstance))
                .map(threadFieldAccesses -> threadFieldAccesses.get(thread))
                .map(fieldAccesses -> fieldAccesses.get(fieldName))
                .orElse(Access.NONE);
    }

    public static synchronized Access getArrayPermission(Thread thread, Object arrayInstance, int indexFrom, int indexTo) {
        assert 0 <= indexFrom && indexFrom < indexTo && indexTo <= Array.getLength(arrayInstance) : "Invalid array range.";

        return Optional.ofNullable(arrayPermissions.get(arrayInstance))
                .map(threadRangeAccesses -> threadRangeAccesses.get(thread))
                .map(rangeAccesses -> rangeAccesses.get(indexFrom, indexTo))
                .orElse(Access.NONE);
    }


    @CalledByInstrumentedCode
    public static void logFieldAccess(Object owningInstance, String fieldName, Access observedAccessLevel) {
        if (owningInstance == Access.class && observedAccessLevel == Access.READ) return; // always allow reading these enum values.

        Thread thread = Thread.currentThread();
        Access grantedAccess = getFieldPermission(thread, owningInstance, fieldName);
        Object formattedOwningInstance = owningInstance instanceof Class<?> clazz ? clazz.getName() : owningInstance;
        logAccess(thread, "%s.%s".formatted(formattedOwningInstance, fieldName), observedAccessLevel, grantedAccess);
    }

    @CalledByInstrumentedCode
    public static void logArrayAccess(Object owningArray, int index, Access observedAccessLevel) {
        logArrayAccess(owningArray, index, index + 1, observedAccessLevel);
    }

    @CalledByInstrumentedCode
    public static void logArrayAccess(Object owningArray, int indexFrom, int indexTo, Access observedAccessLevel) {
        Thread thread = Thread.currentThread();
        Access grantedAccess = getArrayPermission(thread, owningArray, indexFrom, indexTo);
        logAccess(thread, "%s.[%d, %d)".formatted(arrayInstanceToString(owningArray), indexFrom, indexTo), observedAccessLevel, grantedAccess);
    }

    private static void logAccess(Thread thread, String reference, Access observedAccess, Access grantedAccess) {

        String message = String.format("Thread %s: trying to access %s at level %s.", thread.getName(), reference, observedAccess);
        boolean allowed = grantedAccess.covers(observedAccess);
        if (!allowed) {
            message = message + String.format(" Violation! %s permission was requested, but only %s permission was given.", observedAccess, grantedAccess);
        }
        Level level = allowed ? Level.INFO : Level.SEVERE;
        LOGGER.log(level, message);
    }

    private static String arrayInstanceToString(Object array) {
        return switch (array) {
            case byte[] bytes -> Arrays.toString(bytes);
            case boolean[] booleans -> Arrays.toString(booleans);
            case short[] shorts -> Arrays.toString(shorts);
            case int[] ints -> Arrays.toString(ints);
            case char[] chars -> Arrays.toString(chars);
            case long[] longs -> Arrays.toString(longs);
            case float[] floats -> Arrays.toString(floats);
            case double[] doubles -> Arrays.toString(doubles);
            case Object[] objects -> Arrays.deepToString(objects);
            case null -> "null";
            default -> throw new RuntimeException("Unreachable");
        };
    }
}
