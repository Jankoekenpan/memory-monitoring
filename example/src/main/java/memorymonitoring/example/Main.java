package memorymonitoring.example;

import memorymonitoring.runtime.Access;
import memorymonitoring.runtime.ArrayReference;
import memorymonitoring.runtime.Permissions;
import memorymonitoring.runtime.References;

import java.util.Arrays;

public class Main {

    private long instanceField;
    private static int staticField;
    private double instanceDouble = Double.NaN;
    private static long[] staticLongArray = {0L, 1L, 2L};
    private Object[] objectArray = new Object[1];

    public static void main(String[] args) {
        IO.println("Hello, World!");

        // TODO make it so this is also no longer needed.
        Permissions.setArrayPermission(staticLongArray, 0, Access.WRITE);

        Main main = new Main();

        Permissions.setArrayPermission(main.objectArray, 0, Access.WRITE);

        // let's cause some violations on purpose (field):
        Permissions.setFieldPermission(Main.class, "staticField", Access.READ);
        Permissions.setFieldPermission(main, "instanceField", Access.NONE);

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
    }

}
