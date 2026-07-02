package com.mohistmc.youer.util;

import ca.spottedleaf.moonrise.common.util.TickThread;
import java.util.concurrent.ThreadFactory;

/**
 * @author Mgazul
 * @date 2026/7/2 18:11
 */
public class ServerLevelTickExecutorThreadFactory implements ThreadFactory {
    private final String worldName;

    public ServerLevelTickExecutorThreadFactory(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread tickThread = new TickThread.ServerLevelTickThread(r, "serverlevel-tick-worker [" + worldName + "]");

        if (tickThread.isDaemon()) {
            tickThread.setDaemon(false);
        }

        if (tickThread.getPriority() != 5) {
            tickThread.setPriority(5);
        }

        return tickThread;
    }
}