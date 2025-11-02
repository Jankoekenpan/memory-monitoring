package memorymonitoring.runtime;


import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
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
        return "ArrayReference(arrayInstance=" + arrayInstanceToString(arrayInstance.get()) + ", index=" + index + ")";
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
            default -> throw new RuntimeException("Impossible");
        };
    }
}