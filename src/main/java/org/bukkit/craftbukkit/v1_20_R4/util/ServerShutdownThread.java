package org.bukkit.craftbukkit.v1_20_R4.util;

import net.minecraft.server.MinecraftServer;

public class ServerShutdownThread extends Thread {
    private final MinecraftServer server;

    public ServerShutdownThread(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            org.spigotmc.AsyncCatcher.enabled = false; // Spigot
            this.server.close();
        } finally {
            try {
                this.server.reader.getTerminal().restore();
            } catch (Exception e) {
            }
        }
    }
}
