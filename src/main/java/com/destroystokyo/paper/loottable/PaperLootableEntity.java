package com.destroystokyo.paper.loottable;

import net.minecraft.world.entity.vehicle.ContainerEntity;
import org.bukkit.craftbukkit.CraftLootTable;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PaperLootableEntity extends Lootable {

    ContainerEntity getHandle();

    /* Lootable */
    @Override
    default @Nullable LootTable getLootTable() {
        return CraftLootTable.minecraftToBukkit(this.getHandle().getLootTable());
    }

    @Override
    default void setLootTable(final @Nullable LootTable table, final long seed) {
        this.getHandle().setLootTable(CraftLootTable.bukkitToMinecraft(table));
        this.getHandle().setLootTableSeed(seed);
    }

    @Override
    default long getSeed() {
        return this.getHandle().getLootTableSeed();
    }
}
