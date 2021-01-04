package nu.mine.mosher;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class DblGui {
    private final AtomicReference<Thread> events = new AtomicReference<>();

    public static DblGui create() throws InvocationTargetException, InterruptedException {
        final AtomicReference<DblGui> gui = new AtomicReference<>();
        EventQueue.invokeAndWait(() -> {
            gui.set(new DblGui());
            gui.get().init();
        });
        return gui.get();
    }

    private void init() {
        this.events.set(Thread.currentThread());

        try {
            final DblFrame frame = DblFrame.create();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
//        DblMenuBar.create(frame);
    }

    public void waitForEventThread() {
        try {
            this.events.get().join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
