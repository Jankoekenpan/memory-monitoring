package memorymonitoring.runtime;

import java.util.Objects;

// TODO support arrays. might need an ArrayReference class?
public final class FieldReference {

    final Object owningInstance; // for static fields, this would be the java.lang.Class instance.
    final String fieldName;

    public FieldReference(Object owningInstance, String fieldName) {
        this.owningInstance = Objects.requireNonNull(owningInstance);
        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FieldReference that
                && this.owningInstance == that.owningInstance   // intentional reference equality
                && this.fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(owningInstance), fieldName);
    }

    @Override
    public String toString() {
        return "FieldReference(owningInstance=" + owningInstance + ", fieldName=" + fieldName + ")";
    }
}
