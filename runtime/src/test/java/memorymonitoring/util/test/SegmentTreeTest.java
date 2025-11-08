package memorymonitoring.util.test;

import memorymonitoring.runtime.Access;
import memorymonitoring.util.SegmentTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SegmentTreeTest {

    @Test
    public void testWriteRead() {
        SegmentTree<Access> permissionRanges = new SegmentTree<>(24, Access.NONE, Access::weakest);

        permissionRanges.set(0, 6, Access.READ);
        permissionRanges.set(6, 12, Access.WRITE);
        permissionRanges.set(18, 24, Access.READ);

        assertEquals(Access.READ, permissionRanges.get(0, 6));
        assertEquals(Access.WRITE, permissionRanges.get(6, 12));
        assertEquals(Access.READ, permissionRanges.get(1, 5));
        assertEquals(Access.READ, permissionRanges.get(5, 7));
        assertEquals(Access.NONE, permissionRanges.get(17, 19));
        assertEquals(Access.NONE, permissionRanges.get(12, 18));
        assertEquals(Access.READ, permissionRanges.get(23, 24));
        assertEquals(Access.NONE, permissionRanges.get(0, 24));

        permissionRanges.set(12, 24, Access.WRITE);

        assertEquals(Access.WRITE, permissionRanges.get(12, 18));
        assertEquals(Access.WRITE, permissionRanges.get(23, 24));
        assertEquals(Access.READ, permissionRanges.get(0, 24));
    }
}
