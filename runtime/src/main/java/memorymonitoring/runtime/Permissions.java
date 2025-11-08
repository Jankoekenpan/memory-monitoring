package memorymonitoring.runtime;

import memorymonitoring.util.CalledByInstrumentedCode;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Permissions {

    private static final Logger LOGGER = Logger.getLogger(Permissions.class.getName());

    // Reference objects will be gc'ed once their owningInstance is no longer reachable,
    // because FieldReference and ArrayReference objects can only be created through the References factory.
    private static final Map<Reference, Map<Thread, Access>> permissions = Collections.synchronizedMap(new WeakHashMap<>());
    // TODO probably want to split this up into two maps:
    // WeakHashMap<Object, Map<FieldName, WeakHashMap<Thread, FractionalPermission>>>
    // WeakHashMap<ArrayObject, RangeMap<WeakHashMap<Thread, FractionalPermission>>>


    private Permissions() {}

    @CalledByInstrumentedCode
    public static void setFieldPermission(Object owningInstance, String fieldName, Access access) {
        setPermission(References.getFieldReference(owningInstance, fieldName), access);
    }

    // probably not called by instrumented code?
    public static void setArrayPermission(Object arrayInstance, int index, Access access) {
        setPermission(References.getArrayReference(arrayInstance, index), access);
        // TODO
        //setArrayPermission(arrayInstance, index, index + 1, access);
    }

    /**
     * Set permission for the current thread to a range in the array
     * @param arrayInstance the array
     * @param indexFrom starting index - inclusive
     * @param indexTo end index - exclusive
     * @param access the permission level to be set.
     */
    @CalledByInstrumentedCode
    public static void setArrayPermission(Object arrayInstance, int indexFrom, int indexTo, Access access) {

    }

    /** @deprecated prefer {@linkplain #setFieldPermission(Object, String, Access)} or {@linkplain #setArrayPermission(Object, int, Access)}. */
    @Deprecated // TODO remove this one?
    public static void setPermission(Reference reference, Access access) {
        setPermission(Thread.currentThread(), reference, access);
    }

    // TODO overload setFieldPermission with (Thread, Object, String, Access) -> void
    // TODO overload setArrayPermission with (Thread Object, int, Access) -> void
    public static void setPermission(Thread thread, Reference reference, Access accessLevel) {
        // TODO should log or throw an exception when other threads already holds conflicting access.
        // TODO should probably also log / throw when thread tries to upgrade its access level?

        // TODO setting to Access.NONE should remove the entry from the permissions map.

        String message = String.format("Thread %s: Setting %s to access level %s.", thread.getName(), reference, accessLevel);
        LOGGER.info(message);

        permissions.computeIfAbsent(reference, _ -> new WeakHashMap<>()).put(thread, accessLevel);
    }

    private static Access getPermission(Thread thread, Reference reference) {
        Map<Thread, Access> threadAccessesForThisReference = permissions.get(reference);
        if (threadAccessesForThisReference == null) {
            return Access.NONE;
        } else {
            return threadAccessesForThisReference.getOrDefault(thread, Access.NONE);
        }
    }

    @CalledByInstrumentedCode
    public static void logFieldAccess(Object owningInstance, String fieldName, Access accessLevel) {
        logAccess(Thread.currentThread(), References.getFieldReference(owningInstance, fieldName), accessLevel);
    }

    @CalledByInstrumentedCode
    public static void logArrayAccess(Object owningArray, int index, Access accessLevel) {
        logAccess(Thread.currentThread(), References.getArrayReference(owningArray, index), accessLevel);
    }

    private static void logAccess(Thread thread, Reference reference, Access accessLevel) {
        if (reference instanceof FieldReference fieldReference && fieldReference.owningInstance() == Access.class) {
            // Accessing enum constants from the Access class is always allowed. Writing is not possible.
            return;
        }

        String message = String.format("Thread %s: trying to access %s at level %s.", thread.getName(), reference, accessLevel);
        Access actualPermission = getPermission(thread, reference);
        boolean allowed = actualPermission.covers(accessLevel);
        if (!allowed) {
            message = message + String.format(" Violation! %s permission was requested, but only %s permission was given.", accessLevel, actualPermission);
        }
        Level level = allowed ? Level.INFO : Level.SEVERE;
        LOGGER.log(level, message);
    }

}
