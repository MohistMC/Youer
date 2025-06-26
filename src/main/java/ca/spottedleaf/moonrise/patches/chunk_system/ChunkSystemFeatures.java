package ca.spottedleaf.moonrise.patches.chunk_system;

import ca.spottedleaf.moonrise.patches.chunk_system.async_save.AsyncChunkSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class ChunkSystemFeatures {

    public static boolean supportsAsyncChunkSave() {
        // uncertain how to properly pass AsyncSaveData to ChunkSerializer#write
        // additionally, there may be mods hooking into the write() call which may not be thread-safe to call
        return true;
    }

    public static AsyncChunkSaveData getAsyncSaveData(final ServerLevel world, final ChunkAccess chunk) {
        return net.minecraft.world.level.chunk.storage.ChunkSerializer.getAsyncSaveData(world, chunk);
    }

    public static CompoundTag saveChunkAsync(final ServerLevel world, final ChunkAccess chunk, final AsyncChunkSaveData asyncSaveData) {
        return net.minecraft.world.level.chunk.storage.ChunkSerializer.saveChunk(world, chunk, asyncSaveData);
    }

    public static boolean forceNoSave(final ChunkAccess chunk) {
        // support for CB chunk mustNotSave
        return chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk && levelChunk.mustNotSave;
    }

    public static boolean supportsAsyncChunkDeserialization() {
        // as it stands, the current problem with supporting this in Moonrise is that we are unsure that any mods
        // hooking into ChunkSerializer#read() are thread-safe to call
        return true;
    }

    private ChunkSystemFeatures() {}
}
