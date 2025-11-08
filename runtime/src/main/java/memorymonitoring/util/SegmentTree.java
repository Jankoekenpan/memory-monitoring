package memorymonitoring.util;

import org.jspecify.annotations.Nullable;

import java.util.function.BinaryOperator;

public final class SegmentTree<T> {

    private final int size;
    private final BinaryOperator<@Nullable T> combiner;
    private final Node<T> root;

    public SegmentTree(int size, T initialValue, BinaryOperator<@Nullable T> combiner) {
        assert size >= 0 : "size must be positive";

        this.size = size;
        this.combiner = combiner;
        this.root = new Node<>(0, size, initialValue);
    }

    private static class Node<T> {
         private final int start, end; //inclusive, exclusive
         private T value;
         private Node<T> left, right;

         Node(int start, int end, T value) {
             this.start = start;
             this.end = end;
             this.value = value;
         }

         Node<T> ensureLeft() {
             if (left == null) {
                 left = new Node<>(start, mid(), this.value);
                 left.value = this.value;
             }
             return left;
         }

         Node<T> ensureRight() {
             if (right == null) {
                 right = new Node<>(mid(), end, this.value);
                 right.value = this.value;
             }
             return right;
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

        if (end < nodeMid) {
            // entirely in the left half
            set(node.ensureLeft(), start, end, value);
        } else if (start >= nodeMid) {
            // entirely in the right half
            set(node.ensureRight(), start, end, value);
        } else {
            // spawn midpoint
            set(node.ensureLeft(), start, nodeMid, value);
            set(node.ensureRight(), nodeMid, end, value);
        }
    }

    public @Nullable T get(int start, int end) {
        assert 0 <= start && start <= end && end <= size : "Invalid start-end range.";

        return get(root, start, end);
    }

    private @Nullable T get(Node<T> node, int start, int end) {
        if (node == null || start >= node.end || end < node.start) {
            // out of bounds
            return null;
        }
        if (node.start == start && end == node.end) {
            // node exactly covers range
            return node.value;
        }

        int nodeMid = node.mid();

        if (end < nodeMid) {
            // entirely in the left half
            return get(node.left, start, end);
        } else if (start >= nodeMid) {
            // entirely in the left half
            return get(node.right, start, end);
        } else {
            // combine values from left and right halves
            @Nullable T valLeft = get(node.left, start, nodeMid);
            @Nullable T valRight = get(node.right, nodeMid, end);
            return combiner.apply(valLeft, valRight);
        }
    }
}
