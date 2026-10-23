package org.bukkit.craftbukkit.util;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.papermc.paper.log.LoggerShutdown;
import net.minecraft.server.MinecraftServer;

public class ServerShutdownThread extends TickThread {
    private final MinecraftServer server;

    public ServerShutdownThread(MinecraftServer server) {
        super("Server Shutdown Thread");
        this.server = server;
        // FML joins only the server thread; asynchronous shutdown may still need its modules.
        net.neoforged.fml.loading.FMLLoader.getCurrent().addCloseCallback(() -> {
            final Thread shutdownThread = this.server.shutdownThread;
            if (shutdownThread != null && shutdownThread != Thread.currentThread()) {
                com.google.common.util.concurrent.Uninterruptibles.joinUninterruptibly(shutdownThread);
            }
            if (this != Thread.currentThread()) {
                com.google.common.util.concurrent.Uninterruptibles.joinUninterruptibly(this);
            }
        });
    }

    @Override
    public void run() {
        try {
            // Paper start - try to shutdown on main
            server.halt(false, false);
            while (!server.hasStopped() && !server.hasStarted && server.getRunningThread().isAlive()) {
                Thread.sleep(100L);
            }
            for (int i = 1000; i > 0 && !server.hasStopped(); i -= 100) {
                Thread.sleep(100);
            }
            if (server.hasStopped()) {
                while (!server.hasFullyShutdown) Thread.sleep(1000);
                return;
            }
            // Looks stalled, close async
            server.forceTicks = true;
            this.server.close();
            while (!server.hasFullyShutdown) Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // Paper end
        } finally {
            LoggerShutdown.shutdownLogging();
        }
    }
}
