# Memory monitoring

This project contains a proof of concept for permission-based monitoring of field accesses in Java.

The project is split up in 3 modules
1. Memory monitoring runtime api
2. Agent which transforms bytecode for field accesses
3. Toy example with Main method

## Compilation
Make use Apache Maven and JDK 25 (or newer) are installed.
Set your JAVA_HOME environment variable to the JDK 25 directory, so that Maven can make use of it.

In the root directory run the following commands:
```sh
mvn clean package
```

## Running
Run the example main class with the monitoring agent attached:

On Unix-like operating systems:
```sh
java -javaagent:./agent/target/agent-1.0-SNAPSHOT.jar -cp ./runtime/target/runtime-1.0-SNAPSHOT.jar:./example/target/example-1.0-SNAPSHOT.jar memorymonitoring.example.Main
```

On Windows:
```shell
java -javaagent:.\agent\target\agent-1.0-SNAPSHOT.jar -cp .\runtime\target\runtime-1.0-SNAPSHOT.jar;.\example\target\example-1.0-SNAPSHOT.jar memorymonitoring.example.Main
```

## Future work
- Un-hardcode classes to be instrumented.
- Fractional permissions? split & merge support?
