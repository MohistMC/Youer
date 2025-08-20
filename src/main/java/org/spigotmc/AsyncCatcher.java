package org.spigotmc;

import net.minecraft.server.MinecraftServer;

public class AsyncCatcher {

    public static boolean enabled = true;

    public static void catchOp(String reason)
    {
        if (!ca.spottedleaf.moonrise.common.util.TickThread.isTickThread()) // Paper // Paper - rewrite chunk system
        {
            MinecraftServer.LOGGER.error("Thread " + Thread.currentThread().getName() + " failed main thread check: " + reason, new Throwable()); // Paper
            throw new IllegalStateException( "Asynchronous " + reason + "!" );
        }
    }

    public static boolean catchAsync() {
        return !ca.spottedleaf.moonrise.common.util.TickThread.isTickThread();
    }
}
