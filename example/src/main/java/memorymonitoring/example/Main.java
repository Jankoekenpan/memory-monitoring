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
        ArrayReference longsAtIndex1 = References.getArrayReference(staticLongArray, 1);
        Permissions.setPermission(longsAtIndex0, Access.WRITE);
        Permissions.setPermission(longsAtIndex1, Access.READ);

        Main main = new Main();

        ArrayReference objectsAtIndex0 = References.getArrayReference(main.objectArray, 0);
        Permissions.setPermission(objectsAtIndex0, Access.WRITE);

        main.instanceField += 1;
        main.instanceDouble = 1.0;
        Main.staticField += 1;
        IO.println("instanceField = " + main.instanceField);
        IO.println("instanceDouble = " + main.instanceDouble);
        IO.println("staticField = " + Main.staticField);

        staticLongArray[0] += 1L;
        staticLongArray[1] += 1L;
        staticLongArray[2] += 1L;
        IO.println("staticLongArray = " + Arrays.toString(staticLongArray));
        main.objectArray[0] = "Projector Inc.";
        IO.println("objectArray = " + Arrays.toString(main.objectArray));
    }

}
