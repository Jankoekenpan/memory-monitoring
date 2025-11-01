package memorymonitoring.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Permissions {

    private static final Logger LOGGER = Logger.getLogger(Permissions.class.getName());

    private static final Map<FieldAccess, Access> permissions = Collections.synchronizedMap(new HashMap<>());

    private Permissions() {}

    public static void setPermission(FieldReference fieldReference, Access access) {
        setPermission(Thread.currentThread().getId(), fieldReference, access);
    }

    private static void setPermission(long threadId, FieldReference fieldReference, Access access) {
        permissions.put(new FieldAccess(threadId, fieldReference), access);
    }

    // Called by bytecode-transformed code
    public static Access getPermission(FieldReference fieldReference) {
        return getPermission(Thread.currentThread().getId(), fieldReference);
    }

    private static Access getPermission(long threadId, FieldReference fieldReference) {
        return getPermission(new FieldAccess(threadId, fieldReference));
    }

    private static Access getPermission(FieldAccess fieldAccess) {
        if (fieldAccess.fieldReference.owningInstance == Access.class) {
            return Access.READ; // read access for Access.NONE, Access.READ, access.WRITE always allowed.
        } else {
            return permissions.getOrDefault(fieldAccess, Access.NONE);
        }
    }

    // Called by bytecode-transformed code
    public static void logRead(FieldReference fieldReference) {
        logRead(new FieldAccess(Thread.currentThread().getId(), fieldReference));
    }

    // Called by bytecode-transformed code
    public static void logWrite(FieldReference fieldReference) {
        logWrite(new FieldAccess(Thread.currentThread().getId(), fieldReference));
    }

    private static void logRead(FieldAccess fieldAccess) {
        logAccess(fieldAccess, Access.READ);
    }

    private static void logWrite(FieldAccess fieldAccess) {
        logAccess(fieldAccess, Access.WRITE);
    }

    private static void logAccess(FieldAccess fieldAccess, Access accessLevel) {
        String message = String.format("Thread %s: trying to access %s at level %s.", fieldAccess.threadId, fieldAccess.fieldReference, accessLevel);
        Access actualPermission = getPermission(fieldAccess);
        boolean allowed = actualPermission.covers(accessLevel);
        if (!allowed) {
            message = message + String.format(" Violation! %s permission was requested, but only %s permission was given.", accessLevel, actualPermission);
        }
        Level level = allowed ? Level.INFO : Level.SEVERE;
        LOGGER.log(level, message);
    }

    private record FieldAccess(long threadId, FieldReference fieldReference) {}
}
