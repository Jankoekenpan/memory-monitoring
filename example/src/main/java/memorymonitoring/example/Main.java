package memorymonitoring.example;

import memorymonitoring.runtime.Access;
import memorymonitoring.runtime.FieldReference;
import memorymonitoring.runtime.Permissions;
import memorymonitoring.runtime.References;

public class Main {

    private long instanceField;
    private static int staticField;
    private double instanceDouble = Double.NaN;

    public static void main(String[] args) {
        IO.println("Hello, World!");

        FieldReference staticFieldRef = References.getFieldReference(Main.class, "staticField");
        Permissions.setPermission(staticFieldRef, Access.WRITE);
        Main main = new Main();
        FieldReference instanceFieldRef = References.getFieldReference(main, "instanceField");
        Permissions.setPermission(instanceFieldRef, Access.WRITE);

        main.instanceField += 1;
        main.instanceDouble = 1.0;
        Main.staticField += 1;
        IO.println("instanceField = " + main.instanceField);
        IO.println("instanceDouble = " + main.instanceDouble);
        IO.println("staticField = " + Main.staticField);
    }

}
