package com.mohistmc.youer.plugins.world.listener;

import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.plugins.world.commands.WorldsCommands;
import com.mohistmc.youer.plugins.world.utils.ConfigByWorlds;
import com.mohistmc.youer.plugins.world.utils.WorldInventory;
import com.mohistmc.youer.plugins.world.utils.WorldInventoryType;
import com.mohistmc.youer.util.I18n;
import java.util.Random;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Mgazul by MohistMC
 * @date 2023/6/14 14:39:37
 */
public class InventoryClickListener {

    public static WorldInventory worldInventory;

    public static void createWorld(InventoryClickEvent event, Player p) {
        p.closeInventory();
        String worldName = WorldsCommands.type;
        p.sendMessage(ChatColor.GREEN + I18n.as("worldlistener.ICL.worldCreateStart" , worldName));
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            String itemName = itemMeta.getDisplayName();
            boolean isVoid = itemName.equals("VOID");
            boolean isFlat = itemName.equals("FLAT");
            World.Environment environment = isVoid ? Environment.NORMAL : (isFlat ? Environment.NORMAL : Environment.valueOf(itemName));
            WorldCreator wc = new WorldCreator(worldName).environment(environment);
            if (isFlat) {
                wc.type(WorldType.FLAT);
                wc.generator(new WorldAPI.FlatGenerator());
            }
            if (isVoid) wc.generator(new WorldAPI.VoidGenerator());
            wc.seed((new Random()).nextLong());
            wc.environment(environment);

            wc.createWorld();

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                String msg = String.format(I18n.as("worldlistener.ICL.worldCreateFailurePart1") + worldName) + I18n.as("worldlistener.ICL.worldCreateFailurePart2");
                p.sendMessage(ChatColor.RED + msg);
                return;
            }

            Location spawnLocation = world.getSpawnLocation();
            while (!spawnLocation.getBlock().getType().isAir() || !spawnLocation.getBlock().getRelative(BlockFace.UP).getType().isAir()) {
                spawnLocation.add(0, 1, 0);
            }

            world.setSpawnLocation(spawnLocation);
            p.sendMessage(ChatColor.GREEN + I18n.as("worldlistener.ICL.worldCreateSuccess" , worldName));
            try {
                ConfigByWorlds.addWorld(world.getName(), true);
                ConfigByWorlds.addSpawn(spawnLocation);
                if (isVoid) ConfigByWorlds.aVoid(world.getName(), true);
                if (isFlat) ConfigByWorlds.aFlat(world.getName(), true);
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }

    }

    public static void init(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (event.getWhoClicked() instanceof Player p) {
            if (worldInventory != null && worldInventory.getInventory() == inventory) {
                if (worldInventory.worldInventoryType() == WorldInventoryType.CREATE) {
                    event.setCancelled(true);

                    if (itemMeta != null && itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() == 2025604) {
                        createWorld(event, p);
                    }
                } else if (worldInventory.worldInventoryType() == WorldInventoryType.LIST) {
                    event.setCancelled(true);
                    if (event.getCurrentItem() == null) {
                        return;
                    }
                    if (itemMeta != null && itemMeta.hasDisplayName() && itemMeta.getDisplayName().startsWith("§7>>")) {
                        String toSplit = itemMeta.getDisplayName();
                        String[] splitted = toSplit.split("6");
                        if (Bukkit.getWorld(splitted[1]) != null) {
                            ConfigByWorlds.getSpawn(splitted[1], p);
                        } else {
                            WorldsCommands.worldNotExists(p, splitted[1]);
                        }
                    } else if (itemMeta != null && itemMeta.hasDisplayName() && itemMeta.getDisplayName().equals(I18n.as("worldmanage.gui.close"))) {
                        p.closeInventory();
                    }
                }
            }
        }
    }
}
