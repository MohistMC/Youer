package org.spigotmc;

import com.mohistmc.youer.util.I18n;
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
                    log.log(Level.SEVERE, I18n.as("watchdog.serverStoppedResponding"));
                    log.log(Level.SEVERE, I18n.as("watchdog.reportPlugin"));
                    log.log(Level.SEVERE, I18n.as("watchdog.especialHttp"));
                    log.log(Level.SEVERE, I18n.as("watchdog.worldSave"));
                    log.log(Level.SEVERE, I18n.as("watchdog.increaseTimeout"));
                    log.log(Level.SEVERE, I18n.as("watchdog.reportIssue"));
                    log.log(Level.SEVERE, I18n.as("watchdog.includeErrors"));
                    log.log(Level.SEVERE, I18n.as("watchdog.youerVersion", Bukkit.getServer().getVersion()));
                    //
                    if (net.minecraft.world.level.Level.lastPhysicsProblem != null) {
                        log.log(Level.SEVERE, "------------------------------");
                        log.log(Level.SEVERE, I18n.as("watchdog.physicsSuppressed"));
                        log.log(Level.SEVERE, I18n.as("watchdog.physicsNear", net.minecraft.world.level.Level.lastPhysicsProblem));
                    }
                    // Paper end
                } else {
                    log.log(Level.SEVERE, I18n.as("watchdog.notBugShort", Bukkit.getServer().getVersion()));
                    log.log(Level.SEVERE, I18n.as("watchdog.noResponse", (currentTime - lastTick) / 1000));
                }
                // Paper end - Different message for short timeout
                log.log(Level.SEVERE, "------------------------------");
                log.log(Level.SEVERE, I18n.as("watchdog.serverThreadDump")); // Paper
                WatchdogThread.dumpThread(ManagementFactory.getThreadMXBean().getThreadInfo(MinecraftServer.getServer().serverThread.getId(), Integer.MAX_VALUE), log);
                log.log(Level.SEVERE, "------------------------------");
                //
                // Paper start - Only print full dump on long timeouts
                if (isLongTimeout) {
                    log.log(Level.SEVERE, I18n.as("watchdog.entireThreadDump"));
                    ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                    for (ThreadInfo thread : threads) {
                        WatchdogThread.dumpThread(thread, log);
                    }
                } else {
                    log.log(Level.SEVERE, I18n.as("watchdog.notBug"));
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
        log.log( Level.SEVERE, I18n.as("watchdog.currentThread", thread.getThreadName()) );
        log.log( Level.SEVERE, I18n.as("watchdog.threadMeta", thread.getThreadId(),
                thread.isSuspended(), thread.isInNative(), thread.getThreadState()) );
        if ( thread.getLockedMonitors().length != 0 )
        {
            log.log( Level.SEVERE, I18n.as("watchdog.waitingMonitors") );
            for ( MonitorInfo monitor : thread.getLockedMonitors() )
            {
                log.log( Level.SEVERE, I18n.as("watchdog.lockedOn", monitor.getLockedStackFrame()) );
            }
        }
        log.log( Level.SEVERE, I18n.as("watchdog.stack") );
        //
        for ( StackTraceElement stack : thread.getStackTrace() )
        {
            log.log( Level.SEVERE, "\t\t" + stack );
        }
    }
}
