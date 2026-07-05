package com.mohistmc.youer.util;

import java.util.concurrent.locks.LockSupport;
import net.minecraft.server.MinecraftServer;

public class ThreadUtils {

    public static void executeOnMainThread(Runnable runnable) {
        MinecraftServer.getServer().processQueue.add(runnable);
        Thread serverThread = MinecraftServer.getServer().getRunningThread();
        if ("waiting for tasks".equals(LockSupport.getBlocker(serverThread))) {
            LockSupport.unpark(serverThread);
        }
    }
}
