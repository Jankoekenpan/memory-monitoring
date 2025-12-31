# Memory monitoring

This project contains a proof of concept for permission-based monitoring of field accesses in Java.

The project is split up in 3 modules
1. Memory monitoring runtime api
2. Agent which transforms bytecode for field accesses
3. Toy example with Main method

## Compilation
Make sure [Apache Maven](https://maven.apache.org/) and [JDK 25](https://openjdk.org/projects/jdk/25/) (or newer) are installed.
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
- Provide callback based api so that user can handle permission requests in their custom way (so not hardcoded to logging).
- Fractional permissions? split & merge support?
- Find a good way for dealing with instance field assignments before super constructor calls (flexible constructor bodies).
- Optimize away unnecessary permission checks where possible.
- Implement permission transfers automatically for fields annotated with @GuardedBy. Support common cases: Synchronized, ReentrantLock.
  - (javax.annotation.concurrent.GuardedBy from jsr305 has retention CLASS, so it should be analyzable by bytecode readers)
- Case studies: buffered source/sink, casino, memcached challenge(verifythis), hagrid challenge(verifythis), parallel quicksort?
- Measurements: instrumented code performance vs non-instrumented code performance, measure against other approaches from other papers.

## Limitations
- java.lang.invoke.VarHandle api (but maybe MethodHandles.Lookup#findVarHandle and #findStaticVarhandle can be supported with some dataflow analysis?)
- java.lang.invoke.MethodHandles.Lookup api (but maybe #find(Static)Getter and #find(Static)Setter could be supported, will require extra runtime maintenance though, to track what fields those MethodHandles correspond with)
