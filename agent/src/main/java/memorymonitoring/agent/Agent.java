package memorymonitoring.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

    // TODO allow invoker to specify of which classes monitoring should be enabled, through agentArgs?

    // Called when JVM starts (java -javaagent:...)
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            System.out.println("Agent loaded at startup.");
            inst.addTransformer(new FieldUsageTransformer());
            inst.addTransformer(new ArrayUsageTransformer());
            inst.addTransformer(new InitializerTransformer());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

}
