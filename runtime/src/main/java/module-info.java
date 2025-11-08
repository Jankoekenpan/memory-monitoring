module memorymonitoring.runtime {

    requires java.logging;
    requires org.jspecify;

    exports memorymonitoring.runtime;

    exports memorymonitoring.util to memorymonitoring.runtime.test;
}