package com.mohistmc.youer.plugins.ban;

import com.mohistmc.tools.ListUtils;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.api.EnchantmentAPI;
import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.plugins.ban.utils.BanUtils;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class BanListener {

    public static com.mohistmc.youer.plugins.ban.utils.BanSaveInventory openInventory;

    public static void save(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (!player.isOp()) return;
        try {
            Inventory inventory = event.getInventory();
            if (openInventory != null && openInventory.getInventory() == inventory) {
                if (openInventory.getBanType() == BanType.ITEM) {
                    List<String> old = BanConfig.ITEM.getItem();
                    for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                        if (itemStack != null && !itemStack.isEmpty()) {
                            ListUtils.isDuplicate(old, itemStack.getType().getKey().asString());
                        }
                    }
                    BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.ITEM);
                } else if (openInventory.getBanType() == BanType.ENTITY) {
                    List<String> old = BanConfig.ENTITY.getEntity();
                    for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                        if (itemStack != null && !itemStack.isEmpty()) {
                            ItemStack nmsItem = ItemAPI.toNMSItem(itemStack);
                            if (nmsItem.getItem() instanceof SpawnEggItem spawnEggItem) {
                                EntityType<?> entitytype = spawnEggItem.getType(nmsItem);
                                var key = BuiltInRegistries.ENTITY_TYPE.getKey(entitytype);
                                ListUtils.isDuplicate(old, key.toString());
                            }
                        }
                    }
                    BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.ENTITY);
                } else if (openInventory.getBanType() == BanType.ENCHANTMENT) {
                    List<String> old = BanConfig.ENCHANTMENT.getEnchantment();
                    for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                        if (itemStack != null && !itemStack.isEmpty()) {
                            if (EnchantmentAPI.has(itemStack)) {
                                for (Enchantment e : EnchantmentAPI.get(itemStack)) {
                                    ListUtils.isDuplicate(old, e.getKey().asString());
                                }
                            }
                        }
                    }
                    BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.ENCHANTMENT);
                } else if (openInventory.getBanType() == BanType.ITEM_MOSHOU) {
                    for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                        if (itemStack != null && !itemStack.isEmpty()) {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.getInventory().remove(itemStack);
                            }
                            BanConfig.MOSHOU.addMoShou(itemStack.getType().getKey().asString());
                        }
                    }
                }
                if (openInventory.getBanType() == BanType.BLOCK) {
                    List<String> old = BanConfig.BLOCK.getBlock();
                    for (org.bukkit.inventory.ItemStack itemStack : event.getInventory().getContents()) {
                        if (itemStack != null && !itemStack.isEmpty()) {
                            ListUtils.isDuplicate(old, itemStack.getType().getKey().asString());
                        }
                    }
                    BanUtils.saveToYaml(player, ClickType.ADD, old, BanType.BLOCK);
                }
                openInventory = null;
            }
        } catch (Exception e) {
            Youer.LOGGER.warn(I18n.as("bans.add.item.failed"));
            e.printStackTrace();
        }
    }
}
