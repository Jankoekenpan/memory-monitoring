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

    public static void setPermission(Reference reference, Access access) {
        setPermission(Thread.currentThread(), reference, access);
    }

    public static void setPermission(Thread thread, Reference reference, Access access) {
        permissions.computeIfAbsent(reference, _ -> new WeakHashMap<>()).put(thread, access);
    }

    // Called by bytecode-transformed code
    public static Access getPermission(FieldReference fieldReference) {
        return getPermission(Thread.currentThread(), fieldReference);
    }

    private static Access getPermission(Thread thread, Reference reference) {
        Map<Thread, Access> threadAccessesForThisReference = permissions.get(reference);
        if (threadAccessesForThisReference == null) {
            return Access.NONE;
        } else {
            return threadAccessesForThisReference.getOrDefault(thread, Access.NONE);
        }
    }

    // Called by bytecode-transformed code
    public static void logRead(Reference reference) {
        logRead(Thread.currentThread(), reference);
    }

    // Called by bytecode-transformed code
    public static void logWrite(Reference reference) {
        logWrite(Thread.currentThread(), reference);
    }

    private static void logRead(Thread thread, Reference reference) {
        logAccess(thread, reference, Access.READ);
    }

    private static void logWrite(Thread thread, Reference reference) {
        logAccess(thread, reference, Access.WRITE);
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
