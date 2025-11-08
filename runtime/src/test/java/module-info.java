module memorymonitoring.runtime.test {

    requires memorymonitoring.runtime;
    requires org.junit.jupiter.api;

    exports memorymonitoring.util.test to org.junit.platform.commons;
}