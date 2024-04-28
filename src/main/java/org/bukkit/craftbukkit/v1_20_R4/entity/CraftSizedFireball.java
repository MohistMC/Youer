package org.bukkit.craftbukkit.v1_20_R4.entity;

import net.minecraft.world.entity.projectile.Fireball;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R4.CraftServer;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftItemStack;
import org.bukkit.entity.SizedFireball;
import org.bukkit.inventory.ItemStack;

public class CraftSizedFireball extends CraftFireball implements SizedFireball {

    public CraftSizedFireball(CraftServer server, Fireball entity) {
        super(server, entity);
    }

    @Override
    public ItemStack getDisplayItem() {
        if (this.getHandle().getItem().isEmpty()) {
            return new ItemStack(Material.FIRE_CHARGE);
        } else {
            return CraftItemStack.asBukkitCopy(this.getHandle().getItem());
        }
    }

    @Override
    public void setDisplayItem(ItemStack item) {
        this.getHandle().setItem(CraftItemStack.asNMSCopy(item));
    }

    @Override
    public Fireball getHandle() {
        return (Fireball) this.entity;
    }
}
