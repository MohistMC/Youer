package com.mohistmc.youer;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.NamedThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.spigotmc.TicksPerSecondCommand;

public class WatchMohist implements Runnable {

    public static ScheduledThreadPoolExecutor WatchMohist = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("WatchMohist"));

    private static long Time = 0L;
    private static long WarnTime = 0L;

    public static void start() {
        if (isEnable()) {
            WatchMohist.scheduleAtFixedRate(new WatchMohist(), 60000L, 500L, TimeUnit.MILLISECONDS);
        }
    }

    public static void update() {
        if (isEnable()) {
            Time = System.currentTimeMillis();
        }
    }

    public static void stop() {
        if (isEnable()) {
            WatchMohist.shutdown();
        }
    }

    public static boolean isEnable() {
        return MohistConfig.watchdog_mohist;
    }

    @Override
    public void run() {
        long curTime = System.currentTimeMillis();
        if (Time > 0L && curTime - Time > 2000L && curTime - WarnTime > 60000L) {
            WarnTime = curTime;
            Youer.LOGGER.warn(Youer.i18n.as("watchmohist.1"));

            double[] tps = Bukkit.getServer().getTPS();
            String[] tpsAvg = new String[tps.length];
            for (int i = 0; i < tps.length; i++) {
                tpsAvg[i] = TicksPerSecondCommand.format(tps[i]);
            }

            Youer.LOGGER.warn(Youer.i18n.as("watchmohist.2", String.valueOf(curTime - Time), StringUtils.join(tpsAvg, ", ")));
            Youer.LOGGER.warn(Youer.i18n.as("watchmohist.3"));
            Youer.LOGGER.warn(Youer.i18n.as("watchmohist.4"));
            for (StackTraceElement stack : MinecraftServer.getServer().serverThread.getStackTrace()) {
                Youer.LOGGER.warn("{}{}", Youer.i18n.as("watchmohist.5"), stack);
            }
            Youer.LOGGER.warn(Youer.i18n.as("watchmohist.1"));
        }
    }
}