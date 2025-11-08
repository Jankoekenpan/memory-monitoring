package memorymonitoring.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class WeakIdentityHashMap<K, V> {

    private final HashMap<ByIdentityWeakReference<K>, V> map = new HashMap();
    private final ReferenceQueue<ByIdentityWeakReference<K>> referenceQueue = new ReferenceQueue<>();

    private void cleanUp() {
        Reference<?> ref; // is always ByIdentityWeakReference<K>
        while ((ref = referenceQueue.poll()) != null) {
            map.remove(ref);
        }
    }

    public V put(K key, V value) {
        cleanUp();
        return map.put(new ByIdentityWeakReference<>(key), value);
    }

    public V get(K key) {
        cleanUp();
        return map.get(new ByIdentityWeakReference<>(key));
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V value = get(key);
        if (value == null) {
            value = mappingFunction.apply(key);
            put(key, value);
        }
        return value;
    }

    private static final class ByIdentityWeakReference<T> extends WeakReference<T> {

        private final int hashCode;

        private ByIdentityWeakReference(T object) {
            super(object);
            this.hashCode = System.identityHashCode(object);
        }

        private ByIdentityWeakReference(T object, ReferenceQueue<? super T> referenceQueue) {
            super(object, referenceQueue);
            this.hashCode = System.identityHashCode(object);
        }

        @Override
        public boolean equals(Object o) {
            return (o == this) || (o instanceof ByIdentityWeakReference<?> that && this.get() == that.get());
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "ByIdentityWeakReference(" + get() + ")";
        }
    }

}
