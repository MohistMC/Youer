package org.spigotmc;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;

public class WatchdogThread extends ca.spottedleaf.moonrise.common.util.TickThread // Paper - rewrite chunk system
{
    public static final boolean DISABLE_WATCHDOG = Boolean.getBoolean("disable.watchdog"); // Paper - Improved watchdog support
    private static WatchdogThread instance;
    private long timeoutTime;
    private boolean restart;
    private final long earlyWarningEvery; // Paper - Timeout time for just printing a dump but not restarting
    private final long earlyWarningDelay; // Paper
    public static volatile boolean hasStarted; // Paper
    private long lastEarlyWarning; // Paper - Keep track of short dump times to avoid spamming console with short dumps
    private volatile long lastTick;
    private volatile boolean stopping;

    private WatchdogThread(long timeoutTime, boolean restart)
    {
        super( "Watchdog Thread" );
        this.timeoutTime = timeoutTime;
        this.restart = restart;
        earlyWarningEvery = Math.min(io.papermc.paper.configuration.GlobalConfiguration.get().watchdog.earlyWarningEvery, timeoutTime); // Paper
        earlyWarningDelay = Math.min(io.papermc.paper.configuration.GlobalConfiguration.get().watchdog.earlyWarningDelay, timeoutTime); // Paper
    }

    private static long monotonicMillis()
    {
        return System.nanoTime() / 1000000L;
    }

    public static void doStart(int timeoutTime, boolean restart)
    {
        if ( WatchdogThread.instance == null )
        {
            if (timeoutTime <= 0) timeoutTime = 300; // Paper
            WatchdogThread.instance = new WatchdogThread( timeoutTime * 1000L, restart );
            WatchdogThread.instance.start();
        } else
        {
            WatchdogThread.instance.timeoutTime = timeoutTime * 1000L;
            WatchdogThread.instance.restart = restart;
        }
    }

    public static void tick()
    {
        WatchdogThread.instance.lastTick = WatchdogThread.monotonicMillis();
    }

    public static void doStop()
    {
        if ( WatchdogThread.instance != null )
        {
            WatchdogThread.instance.stopping = true;
        }
    }

    @Override
    public void run()
    {
        while ( !this.stopping )
        {
            //
            // Paper start
            Logger log = Bukkit.getServer().getLogger();
            long currentTime = WatchdogThread.monotonicMillis();
            MinecraftServer server = MinecraftServer.getServer();
            if ( this.lastTick != 0 && this.timeoutTime > 0 && WatchdogThread.hasStarted && (!server.isRunning() || (currentTime > this.lastTick + this.earlyWarningEvery && !DISABLE_WATCHDOG) )) // Paper - add property to disable
            {
                boolean isLongTimeout = currentTime > lastTick + timeoutTime || (!server.isRunning() && !server.hasStopped() && currentTime > lastTick + 1000);
                // Don't spam early warning dumps
                if (!isLongTimeout && (earlyWarningEvery <= 0 || !hasStarted || currentTime < lastEarlyWarning + earlyWarningEvery || currentTime < lastTick + earlyWarningDelay))
                    continue;
                if (!isLongTimeout && server.hasStopped())
                    continue; // Don't spam early watchdog warnings during shutdown, we'll come back to this...
                lastEarlyWarning = currentTime;
                if (isLongTimeout) {
                    // Paper end
                    log.log(Level.SEVERE, "------------------------------");
                    log.log(Level.SEVERE, "The server has stopped responding! This is (probably) not a Youer bug.");
                    log.log(Level.SEVERE, "If you see a plugin in the Server thread dump below, then please report it to that author");
                    log.log(Level.SEVERE, "\t *Especially* if it looks like HTTP or MySQL operations are occurring");
                    log.log(Level.SEVERE, "If you see a world save or edit, then it means you did far more than your server can handle at once");
                    log.log(Level.SEVERE, "\t If this is the case, consider increasing timeout-time in spigot.yml but note that this will replace the crash with LARGE lag spikes");
                    log.log(Level.SEVERE, "If you are unsure or still think this is a Paper bug, please report this to https://github.com/MohistMC/Youer/issues");
                    log.log(Level.SEVERE, "Be sure to include ALL relevant console errors and Minecraft crash reports");
                    log.log(Level.SEVERE, "Youer version: " + Bukkit.getServer().getVersion());
                    //
                    if (net.minecraft.world.level.Level.lastPhysicsProblem != null) {
                        log.log(Level.SEVERE, "------------------------------");
                        log.log(Level.SEVERE, "During the run of the server, a physics stackoverflow was supressed");
                        log.log(Level.SEVERE, "near " + net.minecraft.world.level.Level.lastPhysicsProblem);
                    }
                    // Paper end
                } else {
                    log.log(Level.SEVERE, "--- DO NOT REPORT THIS TO PAPER - THIS IS NOT A BUG OR A CRASH  - " + Bukkit.getServer().getVersion() + " ---");
                    log.log(Level.SEVERE, "The server has not responded for " + (currentTime - lastTick) / 1000 + " seconds! Creating thread dump");
                }
                // Paper end - Different message for short timeout
                log.log(Level.SEVERE, "------------------------------");
                log.log(Level.SEVERE, "Server thread dump (Look for plugins here before reporting to Youer!):"); // Paper
                WatchdogThread.dumpThread(ManagementFactory.getThreadMXBean().getThreadInfo(MinecraftServer.getServer().serverThread.getId(), Integer.MAX_VALUE), log);
                log.log(Level.SEVERE, "------------------------------");
                //
                // Paper start - Only print full dump on long timeouts
                if (isLongTimeout) {
                    log.log(Level.SEVERE, "Entire Thread Dump:");
                    ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                    for (ThreadInfo thread : threads) {
                        WatchdogThread.dumpThread(thread, log);
                    }
                } else {
                    log.log(Level.SEVERE, "--- DO NOT REPORT THIS TO PAPER - THIS IS NOT A BUG OR A CRASH ---");
                }
                log.log(Level.SEVERE, "------------------------------");
                if (isLongTimeout) {
                    if ( !server.hasStopped() )
                    {
                        AsyncCatcher.enabled = false; // Disable async catcher incase it interferes with us
                        server.forceTicks = true;
                        if (restart) {
                            RestartCommand.addShutdownHook( SpigotConfig.restartScript );
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!server.hasStopped()) {
                            server.close();
                        }
                    }
                    break;
                } // Paper end
            }

            try
            {
                sleep( 1000 ); // Paper - Reduce check time to every second instead of every ten seconds, more consistent and allows for short timeout
            } catch ( InterruptedException ex )
            {
                this.interrupt();
            }
        }
    }

    private static void dumpThread(ThreadInfo thread, Logger log)
    {
        log.log( Level.SEVERE, "------------------------------" );
        //
        log.log( Level.SEVERE, "Current Thread: " + thread.getThreadName() );
        log.log( Level.SEVERE, "\tPID: " + thread.getThreadId()
                + " | Suspended: " + thread.isSuspended()
                + " | Native: " + thread.isInNative()
                + " | State: " + thread.getThreadState() );
        if ( thread.getLockedMonitors().length != 0 )
        {
            log.log( Level.SEVERE, "\tThread is waiting on monitor(s):" );
            for ( MonitorInfo monitor : thread.getLockedMonitors() )
            {
                log.log( Level.SEVERE, "\t\tLocked on:" + monitor.getLockedStackFrame() );
            }
        }
        log.log( Level.SEVERE, "\tStack:" );
        //
        for ( StackTraceElement stack : thread.getStackTrace() )
        {
            log.log( Level.SEVERE, "\t\t" + stack );
        }
    }
}
