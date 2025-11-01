package memorymonitoring.example;

import memorymonitoring.runtime.Access;
import memorymonitoring.runtime.FieldReference;
import memorymonitoring.runtime.Monitored;
import memorymonitoring.runtime.Permissions;

@Monitored
public class Main {

    private int instanceField;
    private static int staticField;

    public static void main(String[] args) {
        IO.println("Hello, World!");

        Permissions.setPermission(new FieldReference(Main.class, "staticField"), Access.WRITE);
        Main main = new Main();
        Permissions.setPermission(new FieldReference(main, "instanceField"), Access.WRITE);

        main.instanceField += 1;
        Main.staticField += 1;
        IO.println("instanceField = " + main.instanceField);
        IO.println("staticField = " + Main.staticField);
    }

}
