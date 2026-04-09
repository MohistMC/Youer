package com.mohistmc.youer.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityDeathEvent;

public class ItemEntityTools {

    public static List<ItemEntity> convertDrops(EntityDeathEvent event, LivingEntity victim) {
        List<ItemEntity> items = new ArrayList<>();
        CraftLivingEntity entity = (CraftLivingEntity) victim.getBukkitEntity();
        CraftWorld world = (CraftWorld) entity.getWorld();
        for (org.bukkit.inventory.ItemStack stack : event.getDrops()) {
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) continue;
            var loc = event.getEntity().getLocation();
            ItemEntity itemEntity = new ItemEntity(world.getHandle(), loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(stack));
            items.add(itemEntity);
        }
        return items;
    }
}
