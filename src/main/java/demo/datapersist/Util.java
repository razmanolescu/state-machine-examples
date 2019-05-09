package demo.datapersist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;

public class Util {

    public static long WAIT_INTERVAL = 1000L;

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) { }
    }

    public static void process(StateContext<StateMachineConfig.States, StateMachineConfig.Events> context) {
        System.err.println(String.format("Transition %s -> %s   on thread %s", context.getSource().getId().name(), context.getTarget().getId().name(), Thread.currentThread().getName()));
        sleep(WAIT_INTERVAL);
    }

}
