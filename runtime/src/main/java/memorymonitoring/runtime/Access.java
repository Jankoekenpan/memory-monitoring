package memorymonitoring.runtime;

public enum Access {

    NONE,
    READ,
    WRITE;

    public boolean covers(Access other) {
        return other.ordinal() <= this.ordinal();
    }

    public static Access weakest(Access one, Access two) {
        return one.covers(two) ? two : one;
    }
}
