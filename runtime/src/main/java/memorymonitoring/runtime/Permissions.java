package memorymonitoring.runtime;

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

    private Permissions() {}

    public static void setPermission(FieldReference fieldReference, Access access) {
        setPermission(Thread.currentThread(), fieldReference, access);
    }

    public static void setPermission(Thread thread, FieldReference fieldReference, Access access) {
        permissions.computeIfAbsent(fieldReference, _ -> new WeakHashMap<>()).put(thread, access);
    }

    // Called by bytecode-transformed code
    public static Access getPermission(FieldReference fieldReference) {
        return getPermission(Thread.currentThread(), fieldReference);
    }

    private static Access getPermission(Thread thread, FieldReference fieldReference) {
        Map<Thread, Access> threadAccessesForThisField = permissions.get(fieldReference);
        if (threadAccessesForThisField == null) {
            return Access.NONE;
        } else {
            return threadAccessesForThisField.getOrDefault(thread, Access.NONE);
        }
    }

    // Called by bytecode-transformed code
    public static void logRead(FieldReference fieldReference) {
        logRead(Thread.currentThread(), fieldReference);
    }

    // Called by bytecode-transformed code
    public static void logWrite(FieldReference fieldReference) {
        logWrite(Thread.currentThread(), fieldReference);
    }

    private static void logRead(Thread thread, FieldReference fieldReference) {
        logAccess(thread, fieldReference, Access.READ);
    }

    private static void logWrite(Thread thread, FieldReference fieldReference) {
        logAccess(thread, fieldReference, Access.WRITE);
    }

    private static void logAccess(Thread thread, FieldReference fieldReference, Access accessLevel) {
        if (fieldReference.owningInstance() == Access.class) {
            // Reading from the Access class is always allowed. Writing is not possible.
            return;
        }

        String message = String.format("Thread %s: trying to access %s at level %s.", thread.getName(), fieldReference, accessLevel);
        Access actualPermission = getPermission(thread, fieldReference);
        boolean allowed = actualPermission.covers(accessLevel);
        if (!allowed) {
            message = message + String.format(" Violation! %s permission was requested, but only %s permission was given.", accessLevel, actualPermission);
        }
        Level level = allowed ? Level.INFO : Level.SEVERE;
        LOGGER.log(level, message);
    }

}
