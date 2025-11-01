package memorymonitoring.runtime;

public enum Access {

    NONE,
    READ,
    WRITE;

    public boolean covers(Access other) {
        return other.ordinal() <= this.ordinal();
    }
}
