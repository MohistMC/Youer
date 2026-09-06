package com.mohistmc.youer.feature.menu;

import java.io.File;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener {

    public static void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click actions (air, block, or entity)
        if (!event.getAction().name().startsWith("RIGHT_")) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // If main hand is empty, try off hand
        if (item == null || item.getType() == Material.AIR) {
            item = player.getInventory().getItemInOffHand();
        }

        // No item in hand
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Scan all menu config files
        File menuFolder = new File("youer-config/menu");
        if (!menuFolder.exists() || !menuFolder.isDirectory()) {
            return;
        }

        File[] files = menuFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File menuFile : files) {
            try {
                String fileName = menuFile.getName();
                // Remove .yml extension
                if (fileName.toLowerCase().endsWith(".yml")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }

                MenuConfig menuConfig = MenuCommand.loadMenuConfig(fileName);
                if (menuConfig == null) {
                    continue;
                }

                MenuSettings settings = menuConfig.getMenuSettings();
                String openWithItem = settings.getOpenWithItem();
                if (openWithItem == null || openWithItem.isEmpty()) {
                    continue;
                }

                // Parse the configured material name
                Material targetMaterial = parseMaterial(openWithItem);
                if (targetMaterial == null) {
                    continue;
                }

                // Check if the item in hand matches
                if (item.getType() == targetMaterial) {
                    event.setCancelled(true);
                    MenuGUI.openMenu(player, menuConfig);
                    return;
                }
            } catch (Exception e) {
                // Skip invalid menu configs
            }
        }
    }

    private static Material parseMaterial(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return null;
        }
        String formattedName = materialName.replaceAll("[ _-]", "").toUpperCase();
        Material material = Material.matchMaterial(formattedName);
        if (material != null) {
            return material;
        }
        material = Material.matchMaterial(materialName);
        if (material != null) {
            return material;
        }
        return null;
    }
}