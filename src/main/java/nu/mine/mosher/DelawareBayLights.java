package nu.mine.mosher;

import java.lang.reflect.InvocationTargetException;

public class DelawareBayLights {
    public static void main(final String... args) throws InvocationTargetException, InterruptedException {
        final DblGui gui = DblGui.create();
        gui.waitForEventThread();
    }
}
