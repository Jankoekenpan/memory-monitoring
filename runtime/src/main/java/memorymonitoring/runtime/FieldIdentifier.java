package memorymonitoring.runtime;

// TODO should declaringClass be a String? perhaps yes. Right now we are running into the following error:
// TODO Error: Unable to load main class memorymonitoring.example.Main in module memorymonitoring.example
// TODO Caused by: java.lang.LinkageError: loader 'app' attempted duplicate class definition for memorymonitoring.example.Main.
// TODO why would this happen though?
public record FieldIdentifier(Class<?> declaringClass, String fieldName) {
}
