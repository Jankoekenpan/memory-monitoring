package memorymonitoring.runtime;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

/** Obtain instance via {@linkplain References}. */
public final class FieldReference implements Reference {

    private final WeakReference<Object> owningInstance; // for static fields, the owningInstance Object is a java.lang.Class object.
    private final String fieldName;

    private final int hashCode;

    FieldReference(Object owningInstance, String fieldName) {
        this.owningInstance = new WeakReference<>(Objects.requireNonNull(owningInstance));
        this.fieldName = Objects.requireNonNull(fieldName);

        this.hashCode = Objects.hash(System.identityHashCode(owningInstance), fieldName);
    }

    public @Nullable Object owningInstance() {
        return owningInstance.get();
    }

    public String fieldName() {
        return fieldName;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof FieldReference that
                && this.owningInstance.get() == that.owningInstance.get()
                && this.fieldName.equals(that.fieldName));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "FieldReference(owningInstance=" + owningInstance.get() + ", fieldName=" + fieldName + ")";
    }
}
