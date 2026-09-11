package org.bukkit.craftbukkit.block;

import com.google.common.base.Preconditions;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.DecoratedPot;
import org.bukkit.craftbukkit.inventory.CraftInventoryDecoratedPot;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.inventory.DecoratedPotInventory;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

public class CraftDecoratedPot extends CraftBlockEntityState<DecoratedPotBlockEntity> implements DecoratedPot {

    public CraftDecoratedPot(World world, DecoratedPotBlockEntity blockEntity) {
        super(world, blockEntity);
    }

    protected CraftDecoratedPot(CraftDecoratedPot state, Location location) {
        super(state, location);
    }

    @Override
    public DecoratedPotInventory getSnapshotInventory() {
        return new CraftInventoryDecoratedPot(this.getSnapshot());
    }

    @Override
    public DecoratedPotInventory getInventory() {
        if (!this.isPlaced()) {
            return this.getSnapshotInventory();
        }

        return new CraftInventoryDecoratedPot(this.getBlockEntity());
    }

    @Override
    public void setLootTable(final org.bukkit.loot.LootTable table) {
        this.setLootTable(table, this.getSeed());
    }

    @Override
    public void setLootTable(org.bukkit.loot.LootTable table, long seed) {
        this.getSnapshot().setLootTable(org.bukkit.craftbukkit.CraftLootTable.bukkitToMinecraft(table), seed);
    }

    @Override
    public org.bukkit.loot.LootTable getLootTable() {
        return org.bukkit.craftbukkit.CraftLootTable.minecraftToBukkit(this.getSnapshot().getLootTable());
    }

    @Override
    public void setSeed(final long seed) {
        this.getSnapshot().setLootTableSeed(seed);
    }

    @Override
    public long getSeed() {
        return this.getSnapshot().getLootTableSeed();
    }

    @Override
    public void setSherd(Side face, Material sherd) {
        Preconditions.checkArgument(face != null, "face must not be null");
        Preconditions.checkArgument(sherd == null || sherd == Material.BRICK || Tag.ITEMS_DECORATED_POT_SHERDS.isTagged(sherd), "sherd is not a valid sherd material: %s", sherd);

        final Optional<ItemStackTemplate> sherdItem = Optional.ofNullable(sherd != null ? sherd.asItemType() : ItemType.BRICK)
            .map(CraftItemType::bukkitToMinecraftNew)
            .map(ItemStackTemplate::new);
        final PotDecorations decorations = this.getSnapshot().getDecorations();

        switch (face) {
            case BACK -> this.getSnapshot().decorations = new PotDecorations(sherdItem, decorations.left(), decorations.right(), decorations.front());
            case LEFT -> this.getSnapshot().decorations = new PotDecorations(decorations.back(), sherdItem, decorations.right(), decorations.front());
            case RIGHT -> this.getSnapshot().decorations = new PotDecorations(decorations.back(), decorations.left(), sherdItem, decorations.front());
            case FRONT -> this.getSnapshot().decorations = new PotDecorations(decorations.back(), decorations.left(), decorations.right(), sherdItem);
            default -> throw new IllegalArgumentException("Unexpected value: " + face);
        }
    }

    @Override
    public Material getSherd(Side face) {
        Preconditions.checkArgument(face != null, "face must not be null");

        PotDecorations decorations = this.getSnapshot().getDecorations();
        Optional<ItemStackTemplate> sherdItem = switch (face) {
            case BACK -> decorations.back();
            case LEFT -> decorations.left();
            case RIGHT -> decorations.right();
            case FRONT -> decorations.front();
        };

        return sherdItem.map(CraftItemType::minecraftToBukkitNew).map(ItemType::asMaterial).orElse(Material.BRICK);
    }

    @Override
    public Map<Side, Material> getSherds() {
        PotDecorations decorations = this.getSnapshot().getDecorations();

        Map<Side, Material> sherds = new EnumMap<>(Side.class);
        sherds.put(Side.BACK, decorations.back().map(CraftItemType::minecraftToBukkitNew).map(ItemType::asMaterial).orElse(Material.BRICK));
        sherds.put(Side.LEFT, decorations.left().map(CraftItemType::minecraftToBukkitNew).map(ItemType::asMaterial).orElse(Material.BRICK));
        sherds.put(Side.RIGHT, decorations.right().map(CraftItemType::minecraftToBukkitNew).map(ItemType::asMaterial).orElse(Material.BRICK));
        sherds.put(Side.FRONT, decorations.front().map(CraftItemType::minecraftToBukkitNew).map(ItemType::asMaterial).orElse(Material.BRICK));
        return sherds;
    }

    @Override
    public List<Material> getShards() {
        return List.copyOf(this.getSherds().values());
    }

    @Override
    public CraftDecoratedPot copy() {
        return new CraftDecoratedPot(this, null);
    }

    @Override
    public CraftDecoratedPot copy(Location location) {
        return new CraftDecoratedPot(this, location);
    }

    @Override
    public void startWobble(@NotNull final WobbleStyle style) {
        Preconditions.checkArgument(style != null, "style must not be null");
        this.requirePlaced();

        DecoratedPotBlockEntity.WobbleStyle originalStyle = switch (style) {
            case POSITIVE -> DecoratedPotBlockEntity.WobbleStyle.POSITIVE;
            case NEGATIVE -> DecoratedPotBlockEntity.WobbleStyle.NEGATIVE;
        };
        this.getBlockEntity().wobble(originalStyle);
    }
}
