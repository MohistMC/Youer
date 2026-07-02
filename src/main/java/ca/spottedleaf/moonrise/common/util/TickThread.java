package ca.spottedleaf.moonrise.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class TickThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(TickThread.class);
    public static final boolean HARD_THROW = !Boolean.getBoolean("sparklypaper.disableHardThrow"); // SparklyPaper - parallel world ticking - THIS SHOULD NOT BE DISABLED SINCE IT CAN CAUSE DATA CORRUPTION!!! Anyhow, for production servers, if you want to make a test run to see if the server could crash, you can test it with this disabled

    private static String getThreadContext() {
        return "thread=" + Thread.currentThread().getName();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static void ensureTickThread(final String reason) {
        if (!isTickThread()) {
            LOGGER.error("Thread failed main thread check: " + reason + ", context=" + getThreadContext(), new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(reason);
        }
    }

    public static void ensureTickThread(final Level world, final BlockPos pos, final String reason) {
        if (!isTickThreadFor(world, pos)) {
            final String ex = "Thread failed main thread check: " +
                               reason + ", context=" + getThreadContext() + ", world=" + WorldUtil.getWorldName(world) + ", block_pos=" + pos + " - " + getTickThreadInformation(world.getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    public static void ensureTickThread(final Level world, final BlockPos pos, final int blockRadius, final String reason) {
        if (!isTickThreadFor(world, pos, blockRadius)) {
            final String ex = "Thread failed main thread check: " +
                reason + ", context=" + getThreadContext() + ", world=" + WorldUtil.getWorldName(world) + ", block_pos=" + pos + ", block_radius=" + blockRadius + " - " + getTickThreadInformation(world.getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    public static void ensureTickThread(final Level world, final ChunkPos pos, final String reason) {
        if (!isTickThreadFor(world, pos)) {
            final String ex = "Thread failed main thread check: " +
                reason + ", context=" + getThreadContext() + ", world=" + WorldUtil.getWorldName(world) + ", chunk_pos=" + pos + " - " + getTickThreadInformation(world.getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    public static void ensureTickThread(final Level world, final int chunkX, final int chunkZ, final String reason) {
        if (!isTickThreadFor(world, chunkX, chunkZ)) {
            final String ex = "Thread failed main thread check: " +
                reason + ", context=" + getThreadContext() + ", world=" + WorldUtil.getWorldName(world) + ", chunk_pos=" + new ChunkPos(chunkX, chunkZ) + " - " + getTickThreadInformation(world.getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    public static void ensureTickThread(final Entity entity, final String reason) {
        if (!isTickThreadFor(entity)) {
            final String ex = "Thread failed main thread check: " +
                reason + ", context=" + getThreadContext() + ", entity=" + EntityUtil.dumpEntity(entity) + " - " + getTickThreadInformation(entity.level().getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    public static void ensureTickThread(final Level world, final AABB aabb, final String reason) {
        if (!isTickThreadFor(world, aabb)) {
            final String ex = "Thread failed main thread check: " +
                reason + ", context=" + getThreadContext() + ", world=" + WorldUtil.getWorldName(world) + ", aabb=" + aabb + " - " + getTickThreadInformation(world.getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    public static void ensureTickThread(final Level world, final double blockX, final double blockZ, final String reason) {
        if (!isTickThreadFor(world, blockX, blockZ)) {
            final String ex = "Thread failed main thread check: " +
                reason + ", context=" + getThreadContext() + ", world=" + WorldUtil.getWorldName(world) + ", block_pos=" + new Vec3(blockX, 0.0, blockZ) + " - " + getTickThreadInformation(world.getServer());
            LOGGER.error(ex, new Throwable());
            if (HARD_THROW) // SparklyPaper - parallel world ticking
            throw new IllegalStateException(ex);
        }
    }

    // SparklyPaper - parallel world ticking
    // This is an additional method to check if the tick thread is bound to a specific world because, by default, Paper's isTickThread methods do not provide this information
    // Because we only tick worlds in parallel (instead of regions), we can use this for our checks
    public static void ensureTickThread(final net.minecraft.server.level.ServerLevel world, final String reason) {
        if (!isTickThreadFor(world)) {
            LOGGER.error("Thread " + Thread.currentThread().getName() + " failed main thread check: " + reason + " @ world " + world.getWorld().getName() + " - " + getTickThreadInformation(world.getServer()), new Throwable());
            if (HARD_THROW)
                throw new IllegalStateException(reason);
        }
    }

    // SparklyPaper - parallel world ticking
    // This is an additional method to check if it is a tick thread but ONLY a tick thread
    public static void ensureOnlyTickThread(final String reason) {
        boolean isTickThread = isTickThread();
        boolean isServerLevelTickThread = isServerLevelTickThread();
        if (!isTickThread || isServerLevelTickThread) {
            LOGGER.error("Thread " + Thread.currentThread().getName() + " failed main thread ONLY tick thread check: " + reason, new Throwable());
            if (HARD_THROW)
                throw new IllegalStateException(reason);
        }
    }

    // SparklyPaper - parallel world ticking
    // This is an additional method to check if the tick thread is bound to a specific world or if it is an async thread.
    public static void ensureTickThreadOrAsyncThread(final net.minecraft.server.level.ServerLevel world, final String reason) {
        boolean isValidTickThread = isTickThreadFor(world);
        boolean isAsyncThread = !isTickThread();
        boolean isValid = isAsyncThread || isValidTickThread;
        if (!isValid) {
            LOGGER.error("Thread " + Thread.currentThread().getName() + " failed main thread or async thread check: " + reason + " @ world " + world.getWorld().getName() + " - " + getTickThreadInformation(world.getServer()), new Throwable());
            if (HARD_THROW)
                throw new IllegalStateException(reason);
        }
    }

    public static String getTickThreadInformation(net.minecraft.server.MinecraftServer minecraftServer) {
        StringBuilder sb = new StringBuilder();
        Thread currentThread = Thread.currentThread();
        sb.append("Is tick thread? ");
        sb.append(currentThread instanceof TickThread);
        sb.append("; Is server level tick thread? ");
        sb.append(currentThread instanceof ServerLevelTickThread);
        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            sb.append("; Currently ticking level: ");
            if (serverLevelTickThread.currentlyTickingServerLevel != null) {
                sb.append(serverLevelTickThread.currentlyTickingServerLevel.getWorld().getName());
            } else {
                sb.append("null");
            }
        }
        sb.append("; Is iterating over levels? ");
        sb.append(minecraftServer.isIteratingOverLevels);
        sb.append("; Are we going to hard throw? ");
        sb.append(HARD_THROW);
        return sb.toString();
    }

    public static boolean isServerLevelTickThread() {
        return Thread.currentThread() instanceof ServerLevelTickThread;
    }

    public final int id; /* We don't override getId as the spec requires that it be unique (with respect to all other threads) */

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    public TickThread(final String name) {
        this(null, name);
    }

    public TickThread(final Runnable run, final String name) {
        this(null, run, name);
    }

    public TickThread(final ThreadGroup group, final Runnable run, final String name) {
        this(group, run, name, ID_GENERATOR.incrementAndGet());
    }

    private TickThread(final ThreadGroup group, final Runnable run, final String name, final int id) {
        super(group, run, name);
        this.id = id;
    }

    public static TickThread getCurrentTickThread() {
        return (TickThread)Thread.currentThread();
    }

    public static boolean isTickThread() {
        return Thread.currentThread() instanceof TickThread;
    }

    public static boolean isShutdownThread() {
        return false;
    }

    public static boolean isTickThreadFor(final Level world, final BlockPos pos) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final BlockPos pos, final int blockRadius) {
        return isTickThread();
    }

    public static boolean isTickThreadFor(final Level world, final ChunkPos pos) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final Vec3 pos) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final int chunkX, final int chunkZ) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final AABB aabb) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final double blockX, final double blockZ) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final Vec3 position, final Vec3 deltaMovement, final int buffer) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final int fromChunkX, final int fromChunkZ, final int toChunkX, final int toChunkZ) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Level world, final int chunkX, final int chunkZ, final int radius) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    // SparklyPaper - parallel world ticking
    // This is an additional method to check if the tick thread is bound to a specific world because, by default, Paper's isTickThread methods do not provide this information
    // Because we only tick worlds in parallel (instead of regions), we can use this for our checks
    public static boolean isTickThreadFor(final Level world) {
        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == world;
        } else return currentThread instanceof TickThread;
    }

    public static boolean isTickThreadFor(final Entity entity) {
        if (entity == null) {
            return true;
        }

        Thread currentThread = Thread.currentThread();

        if (currentThread instanceof ServerLevelTickThread serverLevelTickThread) {
            return serverLevelTickThread.currentlyTickingServerLevel == entity.level();
        } else return currentThread instanceof TickThread;
    }

    // SparklyPaper start - parallel world ticking
    public static class ServerLevelTickThread extends TickThread {
        public ServerLevelTickThread(String name) {
            super(name);
        }

        public ServerLevelTickThread(Runnable run, String name) {
            super(run, name);
        }

        public net.minecraft.server.level.ServerLevel currentlyTickingServerLevel;
    }
    // SparklyPaper end
}
