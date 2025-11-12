package memorymonitoring.example;

import memorymonitoring.runtime.Access;
import memorymonitoring.runtime.Permissions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

public class Main {

    private static final long CONSTANT_LONG = Long.MAX_VALUE;
    private long instanceField;
    private static int staticField;
    private double instanceDouble = Double.NaN;
    private static long[] staticLongArray = {0L, 1L, 2L};
    private Object[] objectArray = new Object[1];
    private final String string = "string";

    public static void main(String[] args) throws Exception {
        IO.println("Hello, World!");

        Main main = new Main();

        // let's cause some violations on purpose (field):
        Permissions.setFieldPermission(Main.class, Main.class, "staticField", Access.READ);
        Permissions.setFieldPermission(main, Main.class, "instanceField", Access.NONE);

        main.instanceField += 1;
        main.instanceDouble = 1.0;
        Main.staticField += 1;
        IO.println("instanceField = " + main.instanceField);
        IO.println("instanceDouble = " + main.instanceDouble);
        IO.println("staticField = " + Main.staticField);

        // let's cause some violations on purpose (array):
        Permissions.setArrayPermission(staticLongArray, 1, Access.READ);
        Permissions.setArrayPermission(staticLongArray, 2, Access.NONE);

        staticLongArray[0] += 1L;
        staticLongArray[1] += 1L;
        staticLongArray[2] += 1L;
        IO.println("staticLongArray = " + Arrays.toString(staticLongArray));
        main.objectArray[0] = "Projector Inc.";
        IO.println("objectArray = " + Arrays.toString(main.objectArray));

        int[][] matrix = {{1, 2}, {3, 4}};
        matrix[1][1] = 5;
        IO.println("matrix = " + Arrays.deepToString(matrix));

        System.arraycopy(matrix[1], 0, matrix[0], 1, 1);
        IO.println("matrix = " + Arrays.deepToString(matrix));

        int[][] grid = new int[10][10];
        IO.println("grid = " + Arrays.deepToString(grid));

        Array.setInt(grid[0], 0, 1);
        IO.println("grid = " + Arrays.deepToString(grid));

        int x = Array.getInt(grid[0], 0);

        String[] stringArray = (String[]) Array.newInstance(String.class, 1);
        stringArray[0] = "Hello";           // ["Hello"]
        String[][] stringArrayArray = (String[][]) Array.newInstance(String.class, 2, 2);
        stringArrayArray[1][1] = "World!";  // [[null, null], [null, "World!"]]

        System.arraycopy(stringArray, 0, stringArrayArray[0], 0, stringArray.length);   // [["Hello", null], [null, "World"]]
        IO.println(Arrays.deepToString(stringArrayArray));

        Field staticField = Main.class.getDeclaredField("staticField");
        staticField.setInt(null, 1337);
        IO.println(staticField.getInt(null));
        Field instanceField = Main.class.getDeclaredField("instanceField");
        instanceField.setLong(main, 1337L);
        IO.println(instanceField.getLong(main));
    }

}
