package org.spigotmc;

public class AsyncCatcher {

    public static boolean enabled = true;

    public static void catchOp(String reason) {
        if (!ca.spottedleaf.moonrise.common.util.TickThread.isTickThread()) // Paper // Paper - rewrite chunk system
        {
            throw new IllegalStateException("Asynchronous " + reason + "!");
        }
    }

    public static boolean catchAsync() {
        return !ca.spottedleaf.moonrise.common.util.TickThread.isTickThread();
    }
}
