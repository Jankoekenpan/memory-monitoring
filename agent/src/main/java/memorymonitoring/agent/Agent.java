package memorymonitoring.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

    // Called when JVM starts (java -javaagent:...)
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent loaded at startup.");
        inst.addTransformer(new FieldAccessTransformer());
        // TODO fix the following issue:
        /*
          Exception in thread "main" *** java.lang.instrument ASSERTION FAILED ***: "!errorOutstanding" with message Outstanding error when calling method in invokeJavaAgentMainMethod at s\open\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 627
          *** java.lang.instrument ASSERTION FAILED ***: "success" with message invokeJavaAgentMainMethod failed at s\open\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 466
          *** java.lang.instrument ASSERTION FAILED ***: "result" with message agent load/premain call failed at s\open\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 429
         */
        // TODO does it have something to do with the maxStack value of transformed methods, or did the agent itself throw an exception?
    }

}
