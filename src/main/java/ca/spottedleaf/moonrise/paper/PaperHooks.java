package ca.spottedleaf.moonrise.paper;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import ca.spottedleaf.moonrise.paper.util.BaseChunkSystemHooks;
import com.mohistmc.youer.api.ServerAPI;
import com.mohistmc.youer.compat.architectury.MixinChunkMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.function.Predicate;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class PaperHooks extends BaseChunkSystemHooks implements PlatformHooks {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    @Override
    public String getBrand() {
        return "Paper";
    }

    @Override
    public int getLightEmission(final BlockState blockState, final BlockGetter world, final BlockPos pos) {
        return blockState.getLightEmission(world, pos);
    }

    @Override
    public Predicate<BlockState> maybeHasLightEmission() {
        return (final BlockState state) -> {
            return state.hasDynamicLightEmission() || state.getLightEmission(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != 0;
        };
    }

    @Override
    public boolean hasCurrentlyLoadingChunk() {
        return true;
    }

    @Override
    public LevelChunk getCurrentlyLoadingChunk(final GenerationChunkHolder holder) {
        return holder.currentlyLoading;
    }

    @Override
    public void setCurrentlyLoading(final GenerationChunkHolder holder, final LevelChunk levelChunk) {
        holder.currentlyLoading = levelChunk;
    }

    @Override
    public void chunkFullStatusComplete(final LevelChunk newChunk, final ProtoChunk original) {
    }

    @Override
    public boolean allowAsyncTicketUpdates() {
        return true;
    }

    @Override
    public void onChunkHolderTicketChange(final ServerLevel world, final ChunkHolder holder, final int oldLevel, final int newLevel) {
        final ChunkPos pos = holder.getPos();

        EventHooks.fireChunkTicketLevelUpdated(
                world, CoordinateUtils.getChunkKey(pos.x(), pos.z()),
                oldLevel, newLevel, holder
        );
    }

    @Override
    public void chunkUnloadFromWorld(final LevelChunk chunk) {
        NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk));
    }

    @Override
    public void chunkSyncSave(final ServerLevel world, final ChunkAccess chunk, final SerializableChunkData data) {
        NeoForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk, world, data));
    }

    @Override
    public void onChunkWatch(final ServerLevel world, final LevelChunk chunk, final ServerPlayer player) {
        EventHooks.fireChunkWatch(player, chunk, world);
    }

    @Override
    public void onChunkUnWatch(final ServerLevel world, final ChunkPos chunk, final ServerPlayer player) {
        EventHooks.fireChunkUnWatch(player, chunk, world);
    }

    @Override
    public void addToGetEntities(final Level world, final Entity entity, final AABB boundingBox, final Predicate<? super Entity> predicate, final List<Entity> into) {
        final Collection<? extends PartEntity<?>> parts = world.dragonParts();
        if (parts.isEmpty()) {
            return;
        }

        for (final PartEntity<?> part : parts) {
            if (part != entity && part.getBoundingBox().intersects(boundingBox) && (predicate == null || predicate.test(part))) {
                into.add(part);
            }
        }
    }

    @Override
    public <T extends Entity> void addToGetEntities(final Level world, final EntityTypeTest<Entity, T> entityTypeTest, final AABB boundingBox, final Predicate<? super T> predicate, final List<? super T> into, final int maxCount) {
        if (into.size() >= maxCount) {
            // fix neoforge issue: do not add if list is already full
            return;
        }

        final Collection<? extends PartEntity<?>> parts = world.dragonParts();
        if (parts.isEmpty()) {
            return;
        }
        for (final PartEntity<?> part : parts) {
            if (!part.getBoundingBox().intersects(boundingBox)) {
                continue;
            }
            final T casted = (T)entityTypeTest.tryCast(part);
            if (casted != null && (predicate == null || predicate.test(casted))) {
                into.add(casted);
                if (into.size() >= maxCount) {
                    break;
                }
            }
        }
    }

    @Override
    public void entityMove(final Entity entity, final long oldSection, final long newSection) {
        CommonHooks.onEntityEnterSection(entity, oldSection, newSection);
    }

    @Override
    public boolean screenEntity(final ServerLevel world, final Entity entity, final boolean fromDisk, final boolean event) {
        if (event && NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, entity.level(), fromDisk)).isCanceled()) {
            return false;
        }
        return true;
    }

    @Override
    public long[] getCounterTypesUncached(final net.minecraft.server.level.TicketType type) {
        final it.unimi.dsi.fastutil.longs.LongArrayList ret = new it.unimi.dsi.fastutil.longs.LongArrayList();
        if (type == TicketType.FORCED) {
            ret.add(ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType.COUNTER_TYPE_FORCED);
        }
        if (type.shouldKeepDimensionActive()) {
            ret.add(ca.spottedleaf.moonrise.patches.chunk_system.ticket.ChunkSystemTicketType.COUNTER_TYPE_KEEP_DIMENSION_ACTIVE);
        }

        return ret.toLongArray();
    }

    @Override
    public boolean configFixMC224294() {
        return true;
    }

    @Override
    public boolean configAutoConfigSendDistance() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().chunkLoadingAdvanced.autoConfigSendDistance;
    }

    @Override
    public double configPlayerMaxLoadRate() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkLoadRate;
    }

    @Override
    public double configPlayerMaxGenRate() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkGenerateRate;
    }

    @Override
    public double configPlayerMaxSendRate() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkSendRate;
    }

    @Override
    public int configPlayerMaxConcurrentLoads() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().chunkLoadingAdvanced.playerMaxConcurrentChunkLoads;
    }

    @Override
    public int configPlayerMaxConcurrentGens() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().chunkLoadingAdvanced.playerMaxConcurrentChunkGenerates;
    }

    @Override
    public long configAutoSaveInterval(final ServerLevel world) {
        return world.paperConfig().chunks.autoSaveInterval.value();
    }

    @Override
    public int configMaxAutoSavePerTick(final ServerLevel world) {
        return world.paperConfig().chunks.maxAutoSaveChunksPerTick;
    }

    @Override
    public int configMinChunkUnloadCount(final ServerLevel world) {
        return 50;
    }

    @Override
    public double configMinChunkUnloadFraction(final ServerLevel world) {
        return 0.05;
    }

    @Override
    public boolean configFixMC159283() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().misc.fixFarEndTerrainGeneration;
    }

    @Override
    public boolean forceNoSave(final ChunkAccess chunk) {
        return chunk instanceof LevelChunk levelChunk && levelChunk.mustNotSave;
    }

    @Override
    public CompoundTag convertNBT(final DSL.TypeReference type, final DataFixer dataFixer, final CompoundTag nbt,
                                  final int fromVersion, final int toVersion) {
        // Paper start - optimise data conversion
        if (type == net.minecraft.util.datafix.fixes.References.PLAYER) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.PLAYER, nbt, fromVersion, toVersion
            );
        }
        if (type == net.minecraft.util.datafix.fixes.References.CHUNK) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.CHUNK, nbt, fromVersion, toVersion
            );
        }
        if (type == net.minecraft.util.datafix.fixes.References.STRUCTURE) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.STRUCTURE, nbt, fromVersion, toVersion
            );
        }
        if (type == net.minecraft.util.datafix.fixes.References.POI_CHUNK) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.POI_CHUNK, nbt, fromVersion, toVersion
            );
        }
        if (type == net.minecraft.util.datafix.fixes.References.ENTITY_CHUNK) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ENTITY_CHUNK, nbt, fromVersion, toVersion
            );
        }
        if (type == net.minecraft.util.datafix.fixes.References.ITEM_STACK) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ITEM_STACK, nbt, fromVersion, toVersion
            );
        }
        if (type == net.minecraft.util.datafix.fixes.References.ENTITY || type == net.minecraft.util.datafix.fixes.References.ENTITY_TREE) {
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ENTITY, nbt, fromVersion, toVersion
            );
        }
        // Paper end - optimise data conversion
        return (CompoundTag)dataFixer.update(
                type, new Dynamic<>(NbtOps.INSTANCE, nbt), fromVersion, toVersion
        ).getValue();
    }

    @Override
    public boolean hasMainChunkLoadHook() {
        return false;
    }

    @Override
    public void mainChunkLoad(final ChunkAccess chunk, final SerializableChunkData chunkData) {
        Event event = new ChunkDataEvent.Load(chunk, chunkData);
        if (ServerAPI.hasMod("architectury")) {
            event = MixinChunkMap.modifyProtoChunkLevel(event, chunk.getLevel());
        }
        NeoForge.EVENT_BUS.post(event);
    }

    @Override
    public List<Entity> modifySavedEntities(final ServerLevel world, final int chunkX, final int chunkZ, final List<Entity> entities) {
        return entities;
    }

    @Override
    public void unloadEntity(final Entity entity) {
        entity.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK, org.bukkit.event.entity.EntityRemoveEvent.Cause.UNLOAD);
    }

    @Override
    public void postLoadProtoChunk(final ServerLevel world, final ProtoChunk chunk) {
        try (final net.minecraft.util.ProblemReporter.ScopedCollector scopedCollector = new net.minecraft.util.ProblemReporter.ScopedCollector(chunk.problemPath(), LOGGER)) {
            net.minecraft.world.level.chunk.status.ChunkStatusTasks.postLoadProtoChunk(world, net.minecraft.world.level.storage.TagValueInput.create(scopedCollector, world.registryAccess(), chunk.getEntities()), chunk.getPos()); // Paper - rewrite chunk system - add ChunkPos param
        }
    }

    @Override
    public int modifyEntityTrackingRange(final Entity entity, final int currentRange) {
        return org.spigotmc.TrackingRange.getEntityTrackingRange(entity, currentRange);
    }

    @Override
    public boolean addTicketForEnderPearls(final ServerLevel world) {
        return !world.paperConfig().misc.legacyEnderPearlBehavior;
    }
}
