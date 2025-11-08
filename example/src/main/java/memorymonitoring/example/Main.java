package memorymonitoring.example;

import memorymonitoring.runtime.Access;
import memorymonitoring.runtime.ArrayReference;
import memorymonitoring.runtime.FieldReference;
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
        ArrayReference longsAtIndex0 = References.getArrayReference(staticLongArray, 0);
        Permissions.setPermission(longsAtIndex0, Access.WRITE);

        Main main = new Main();

        ArrayReference objectsAtIndex0 = References.getArrayReference(main.objectArray, 0);
        Permissions.setPermission(objectsAtIndex0, Access.WRITE);

        // let's cause some violations on purpose (field):
        Permissions.setPermission(References.getFieldReference(Main.class, "staticField"), Access.READ);
        Permissions.setPermission(References.getFieldReference(main, "instanceField"), Access.NONE);

        main.instanceField += 1;
        main.instanceDouble = 1.0;
        Main.staticField += 1;
        IO.println("instanceField = " + main.instanceField);
        IO.println("instanceDouble = " + main.instanceDouble);
        IO.println("staticField = " + Main.staticField);

        // let's cause some violations on purpose (array):
        ArrayReference longsAtIndex1 = References.getArrayReference(staticLongArray, 1);
        ArrayReference longsAtIndex2 = References.getArrayReference(staticLongArray, 2);
        Permissions.setPermission(longsAtIndex1, Access.READ);
        Permissions.setPermission(longsAtIndex2, Access.NONE);

        staticLongArray[0] += 1L;
        staticLongArray[1] += 1L;
        staticLongArray[2] += 1L;
        IO.println("staticLongArray = " + Arrays.toString(staticLongArray));
        main.objectArray[0] = "Projector Inc.";
        IO.println("objectArray = " + Arrays.toString(main.objectArray));
    }

}
