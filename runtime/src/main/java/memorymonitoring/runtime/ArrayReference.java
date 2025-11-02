package memorymonitoring.runtime;


import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

/** Obtain instance via {@linkplain References}. */
public final class ArrayReference implements Reference {
    private final WeakReference<Object> arrayInstance;
    private final int index;

    private final int hashCode;

    ArrayReference(Object arrayInstance, int index) {
        this.arrayInstance = new WeakReference<>(Objects.requireNonNull(arrayInstance));
        this.index = index;

        this.hashCode = Objects.hash(System.identityHashCode(arrayInstance), index);
    }

    public @Nullable Object arrayInstance() {
        return arrayInstance.get();
    }

    public int index() {
        return index;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof ArrayReference that
                && this.arrayInstance.get() == that.arrayInstance.get()
                && this.index == that.index);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ArrayReference(arrayInstance=" + arrayInstance.get() + ", index=" + index + ")";
    }
}