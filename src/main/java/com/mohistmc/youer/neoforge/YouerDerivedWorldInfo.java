package com.mohistmc.youer.neoforge;

import com.mojang.serialization.Lifecycle;
import java.util.UUID;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.timers.TimerQueue;
import org.jetbrains.annotations.NotNull;

public class YouerDerivedWorldInfo extends PrimaryLevelData {

    private final ServerLevelData derivedWorldInfo;

    public YouerDerivedWorldInfo(ServerLevelData derivedWorldInfo, LevelSettings p_78470_, SpecialWorldProperty p_252268_, Lifecycle p_78472_) {
        super(p_78470_, p_252268_, p_78472_);
        this.derivedWorldInfo = derivedWorldInfo;
    }

    public static YouerDerivedWorldInfo create(ServerLevelData worldInfo) {
        return new YouerDerivedWorldInfo(worldInfo, worldSettings(worldInfo), specialWorldProperty(worldInfo), lifecycle(worldInfo));
    }

    private static LevelSettings worldSettings(ServerLevelData data) {
        data = resolveDelegate(data);

        if (data instanceof PrimaryLevelData bridged) {
            return bridged.getLevelSettings();
        }

        if (data instanceof WorldData p) {
            return p.getLevelSettings();
        }

        return new LevelSettings(data.getLevelName(), data.getGameType(), new LevelSettings.DifficultySettings(data.getDifficulty(), data.isHardcore(), data.isDifficultyLocked()), data.isAllowCommands(), WorldDataConfiguration.DEFAULT);
    }

    private static SpecialWorldProperty specialWorldProperty(ServerLevelData data) {
        data = resolveDelegate(data);

        if (data instanceof WorldData d) {
            return (d.isFlatWorld() ?
                    SpecialWorldProperty.FLAT :
                    (d.isDebugWorld() ?
                            SpecialWorldProperty.DEBUG :
                            SpecialWorldProperty.NONE));
        }

        return SpecialWorldProperty.NONE;
    }

    private static Lifecycle lifecycle(ServerLevelData data) {
        data = resolveDelegate(data);
        if (data instanceof PrimaryLevelData bridged) {
            return bridged.worldGenSettingsLifecycle();
        }

        if (data instanceof WorldData p) {
            return p.worldGenSettingsLifecycle();
        }

        return Lifecycle.stable();
    }

    private static ServerLevelData resolveDelegate(ServerLevelData data) {
        if (data instanceof DerivedLevelData bridged) {
            return resolveDelegate(bridged.wrapped);
        }

        return data;
    }

    @Override
    public long getGameTime() {
        return derivedWorldInfo.getGameTime();
    }

    @Override
    public void setGameTime(long time) {
        derivedWorldInfo.setGameTime(time);
    }

    @Override
    public @NotNull String getLevelName() {
        return derivedWorldInfo.getLevelName();
    }

    @Override
    public @NotNull GameType getGameType() {
        return derivedWorldInfo.getGameType();
    }

    @Override
    public void setGameType(@NotNull GameType type) {
        derivedWorldInfo.setGameType(type);
    }

    @Override
    public boolean isHardcore() {
        return derivedWorldInfo.isHardcore();
    }

    @Override
    public boolean isInitialized() {
        return derivedWorldInfo.isInitialized();
    }

    @Override
    public void setInitialized(boolean initializedIn) {
        derivedWorldInfo.setInitialized(initializedIn);
    }

    @Override
    public @NotNull Difficulty getDifficulty() {
        return derivedWorldInfo.getDifficulty();
    }

    @Override
    public boolean isDifficultyLocked() {
        return derivedWorldInfo.isDifficultyLocked();
    }

    @Override
    public void fillCrashReportCategory(@NotNull CrashReportCategory p_164972_, @NotNull LevelHeightAccessor p_164973_) {
        derivedWorldInfo.fillCrashReportCategory(p_164972_, p_164973_);
    }
}
