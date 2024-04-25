package org.bukkit.craftbukkit.v1_20_R4.entity;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.animal.horse.Markings;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftInventoryHorse;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.HorseInventory;

public class CraftHorse extends CraftAbstractHorse implements Horse {

    public CraftHorse(CraftServer server, net.minecraft.world.entity.animal.horse.Horse entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.horse.Horse getHandle() {
        return (net.minecraft.world.entity.animal.horse.Horse) super.getHandle();
    }

    @Override
    public Variant getVariant() {
        return Variant.HORSE;
    }

    @Override
    public Color getColor() {
        return Color.values()[this.getHandle().getVariant().getId()];
    }

    @Override
    public void setColor(Color color) {
        Preconditions.checkArgument(color != null, "Color cannot be null");
        this.getHandle().setVariantAndMarkings(net.minecraft.world.entity.animal.horse.Variant.byId(color.ordinal()), this.getHandle().getMarkings());
    }

    @Override
    public Style getStyle() {
        return Style.values()[this.getHandle().getMarkings().getId()];
    }

    @Override
    public void setStyle(Style style) {
        Preconditions.checkArgument(style != null, "Style cannot be null");
        this.getHandle().setVariantAndMarkings(this.getHandle().getVariant(), Markings.byId(style.ordinal()));
    }

    @Override
    public boolean isCarryingChest() {
        return false;
    }

    @Override
    public void setCarryingChest(boolean chest) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public HorseInventory getInventory() {
        return new CraftInventoryHorse(this.getHandle().inventory);
    }

    @Override
    public String toString() {
        return "CraftHorse{variant=" + this.getVariant() + ", owner=" + this.getOwner() + '}';
    }
}