package com.mohistmc.youer.plugins.world.utils;

import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.plugins.world.commands.WorldsCommands;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Mgazul by MohistMC
 * @date 2023/6/14 14:57:28
 */
public class WorldsGUI {

    public static void openWorldGui(Player p, String name) {
        DemoGUI wh = new DemoGUI(name);
        for (World w : Bukkit.getWorlds()) {
            List<String> infoLore = new ArrayList<>();
            FileConfiguration config = ConfigByWorlds.config;
            boolean flat = false;
            if (ConfigByWorlds.f.exists() && config.getConfigurationSection("worlds.") != null) {
                String worldtype = w.getEnvironment() == null ? "null" : w.getEnvironment().name();
                String infos = "§7-/-";
                String name1 = w.getName();
                String difficulty = w.getDifficulty().name();

                if (config.get("worlds." + w.getName() + ".info") != null) {
                    infos = config.getString("worlds." + w.getName() + ".info", "§7-/-");
                    worldtype = config.getString("worlds." + w.getName() + ".environment");
                    name1 = config.getString("worlds." + w.getName() + ".name", w.getName());
                    difficulty = config.getString("worlds." + w.getName() + ".difficulty");
                }
                infoLore.add(I18n.as("worldmanage.gui.lore0") + name1);
                infoLore.add(I18n.as("worldmanage.gui.lore1") + infos);
                infoLore.add(I18n.as("worldmanage.gui.lore2") + w.getWorldBorder().getSize());
                infoLore.add(I18n.as("worldmanage.gui.lore3") + worldtype);
                infoLore.add(I18n.as("worldmanage.gui.lore4") + difficulty);
                if (w.isMods()) {
                    infoLore.add("§bModid §8>> §7" + w.getModid());
                }
                if (w.isBukkit()) {
                    infoLore.add("§bPluginWorld §8>> §7" + w.isBukkit());
                }
                if (config.get("worlds." + w.getName() + ".void") != null) {
                    infoLore.add("§bVoid §8>> §7" + config.getBoolean("worlds." + w.getName() + ".void"));
                }
                if (config.get("worlds." + w.getName() + ".flat") != null) {
                    flat = config.getBoolean("worlds." + w.getName() + ".flat");
                    infoLore.add("§bFlat §8>> §7" + flat);
                }
            }
            Material material = flat ? Material.GREEN_CARPET : getMaterial(w);
            wh.addItem(new GUIItem(new ItemStackFactory(material)
                               .setDisplayName("§7>> §6" + w.getName())
                               .setLore(infoLore)
                               .build()) {
                           @Override
                           public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                               ItemMeta itemMeta = itemStack.getItemMeta();
                               if (itemMeta != null) {
                                   String worldName = w.getName();
                                   if (Bukkit.getWorld(worldName) != null) {
                                       ConfigByWorlds.getSpawn(worldName, p);
                                   } else {
                                       WorldsCommands.worldNotExists(p, worldName);
                                   }
                               }
                           }
                       }
            );
        }
        wh.openGUI(p);
    }

    public static Material getMaterial(String type) {
        Material material = Material.GRASS_BLOCK;
        switch (type) {
            case "VOID":
                material = Material.BARRIER;
                break;
            case "FLAT":
                material = Material.GREEN_CARPET;
                break;
            default:
        }
        return material;
    }

    public static Material getMaterial(World w) {
        if (w.isFlat()) {
            return Material.GREEN_CARPET;
        }
        if (w.isVoid()) {
            return Material.BARRIER;
        }
        return getMaterial(w.getEnvironment());
    }

    public static Material getMaterial(World.Environment environment) {
        Material material = Material.GRASS_BLOCK;
        if (environment == null) {
            return material;
        }
        switch (environment.name()) {
            case "NETHER":
                material = Material.NETHERRACK;
                break;
            case "THE_END":
                material = Material.END_STONE;
                break;
            case "CUSTOM":
                material = Material.STRUCTURE_VOID;
                break;
            case "TWILIGHTFOREST_TWILIGHT_FOREST":
                material = Material.getMaterial("TWILIGHTFOREST_TWILIGHT_PORTAL_MINIATURE_STRUCTURE");
                break;
            default:
        }
        return material;
    }

    public static void createWorld(String worldName, ItemStack itemStack, Player p) {
        p.closeInventory();
        p.sendMessage(ChatColor.GREEN + I18n.as("worldlistener.ICL.worldCreateStart", worldName));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            String itemName = itemMeta.getDisplayName();
            boolean isVoid = itemName.equals("VOID");
            boolean isFlat = itemName.equals("FLAT");
            World.Environment environment = isVoid ? World.Environment.NORMAL : (isFlat ? World.Environment.NORMAL : World.Environment.valueOf(itemName));
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
            p.sendMessage(ChatColor.GREEN + I18n.as("worldlistener.ICL.worldCreateSuccess", worldName));
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

}
