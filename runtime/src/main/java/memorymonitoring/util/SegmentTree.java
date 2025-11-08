package memorymonitoring.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.BinaryOperator;

public final class SegmentTree<@NonNull T> {

    private final int size;
    private final BinaryOperator<@Nullable T> retrievalCombiner;
    private final Node<T> root;

    public SegmentTree(int size, T initialValue, BinaryOperator<@Nullable T> retrievalCombiner) {
        assert size >= 0 : "size must be positive";
        assert initialValue != null : "initial value cannot be null";

        this.size = size;
        this.retrievalCombiner = retrievalCombiner;
        this.root = new Node<>(0, size, initialValue);
    }

    private static class Node<T> {
         // Invariant: ((value == null) == (left != null)) && ((value == null) == (right != null))

         private final int start, end; //inclusive, exclusive
         private T value;
         private Node<T> left, right;

         Node(int start, int end, T value) {
             this.start = start;
             this.end = end;
             this.value = value;
         }

         void split() {
             if (value != null) {
                 left = new Node<>(start, mid(), value);
                 right = new Node<>(mid(), end, value);
                 value = null;
             }
         }

         int mid() {
             return (start + end) >>> 1;
         }
    }

    public void set(int start, int end, T value) {
        assert 0 <= start && start <= end && end <= size : "Invalid start-end range.";

        set(root, start, end, value);
    }

    private void set(Node<T> node, int start, int end, T value) {
        if (start == end) {
            return;
        }

        if (node.start == start && node.end == end) {
            node.value = value;
            node.left = null;
            node.right = null;
            return;
        }

        int nodeMid = node.mid();
        node.split();
        if (end < nodeMid) {
            // entirely in the left half
            set(node.left, start, end, value);
        } else if (start >= nodeMid) {
            // entirely in the right half
            set(node.right, start, end, value);
        } else {
            // range overlaps with both halves
            set(node.left, start, nodeMid, value);
            set(node.right, nodeMid, end, value);
        }
    }

    public @Nullable T get(int start, int end) {
        assert 0 <= start && start <= end && end <= size : "Invalid start-end range.";

        T value = get(root, start, end);
        return value;
    }

    private @Nullable T get(Node<T> node, int start, int end) {
        if (node == null || start >= node.end || end < node.start) {
            // out of bounds
            return null;
        }

        int nodeMid = node.mid();

        if (node.value != null && node.start <= start && end <= node.end) {
            // node covers range and it has a value
            return node.value;
        }

        T result; // improve debugging.
        if (end <= nodeMid) {
            // entirely in the left half
            result = get(node.left, start, end);
        } else if (start >= nodeMid) {
            // entirely in the left half
            result = get(node.right, start, end);
        } else {
            // combine values from left and right halves
            @Nullable T valLeft = get(node.left, start, nodeMid);
            @Nullable T valRight = get(node.right, nodeMid, end);
            result = retrievalCombiner.apply(valLeft, valRight);
        }
        return result;
    }
}
