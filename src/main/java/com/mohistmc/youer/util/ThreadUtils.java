package com.mohistmc.youer.util;

import java.util.concurrent.locks.LockSupport;
import net.minecraft.server.MinecraftServer;

public class ThreadUtils {

    public static void executeOnMainThread(Runnable runnable) {
        MinecraftServer.getServer().processQueue.add(runnable);
        if (LockSupport.getBlocker(MinecraftServer.getServer().getRunningThread()) == "waiting for tasks") {
            LockSupport.unpark(MinecraftServer.getServer().getRunningThread());
        }
    }
}
