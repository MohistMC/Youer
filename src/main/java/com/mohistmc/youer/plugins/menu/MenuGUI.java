// MenuGUI.java
package com.mohistmc.youer.plugins.menu;

import com.mohistmc.youer.api.gui.DefaultGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.GUIType;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class MenuGUI {

    public static void openMenu(Player player, MenuConfig menuConfig) {
        MenuSettings settings = menuConfig.getMenuSettings();
        String menuName = settings.getName() != null ? settings.getName() : "Menu";

        DefaultGUI gui = new DefaultGUI(GUIType.fromRows(settings.getRows()), menuName);

        Map<String, Icon> icons = menuConfig.getIcons();
        for (Map.Entry<String, Icon> entry : icons.entrySet()) {
            Icon icon = entry.getValue();

            Material material = parseMaterial(icon.getMaterial());
            if (material == null) {
                material = Material.STONE;
            }

            ItemStackFactory itemFactory = new ItemStackFactory(material)
                    .setDisplayName(icon.getName() != null ? icon.getName() : "Unnamed Item");
            itemFactory.player(player);
            if (icon.getLore() != null && !icon.getLore().isEmpty()) {
                List<String> lore = icon.getLore();
                itemFactory.setLore(lore);
            }

            if (icon.getAmount() != null && icon.getAmount() > 1) {
                itemFactory.setAmount(icon.getAmount());
            }

            if (icon.getDurability() != null) {
                itemFactory.setDurability(icon.getDurability().shortValue());
            }

            if (icon.getCustomModelData() != null) {
                itemFactory.customModelData(icon.getCustomModelData());
            }

            if (icon.isHideTooltip()) {
                itemFactory.hideTooltip();
            }

            if (itemFactory.isSkull()) {
                itemFactory.head(icon.getBase64());
            }

            if (icon.getEnchantments() != null && !icon.getEnchantments().isEmpty()) {
                for (String enchantment : icon.getEnchantments()) {
                    try {
                        String enchantmentf = enchantment.split("\\|")[0];
                        String enchantmentL = enchantment.split("\\|")[1];
                        Enchantment enchantmentK = Enchantment.getByName(enchantmentf);
                        int level = Integer.parseInt(enchantmentL);
                        if (enchantmentK != null) {
                            itemFactory.setEnchantment(enchantmentK, level);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (icon.getItemFlags() != null && !icon.getItemFlags().isEmpty()) {
                for (ItemFlag itemFlag : icon.getItemFlags()) {
                    try {
                        itemFactory.addItemFlags(itemFlag);
                    } catch (Exception ignored) {
                    }
                }
            }

            ItemStack itemStack = itemFactory.build();

            int slot = (icon.getPositionY() - 1) * 9 + (icon.getPositionX() - 1);
            gui.setItem(slot, new GUIItem(itemStack) {
                @Override
                public void ClickAction(ClickType type, Player p, ItemStack clickedItem) {
                    handleIconClick(p, icon);

                    if (!icon.isKeepOpen()) {
                        p.closeInventory();
                    }
                }
            });
        }

        gui.openGUI(player);
        if (menuConfig.getMenuSettings().getOpenActions() != null && !menuConfig.getMenuSettings().getOpenActions().isEmpty()) {
            for (String action : menuConfig.getMenuSettings().getOpenActions()) {
                processAction(player, action);
            }
        }
    }

    private static void handleIconClick(Player player, Icon icon) {
        if (icon.getActions() != null && !icon.getActions().isEmpty()) {
            for (String action : icon.getActions()) {
                processAction(player, action);
            }
        }
    }

    private static void processAction(Player player, String action) {
        if (action.startsWith("tell:")) {
            String message = action.substring(5).trim();
            player.sendMessage(message);
        } else if (action.equals("player:")) {
            String cmd = action.substring(7).trim();
            player.performCommand(cmd);
        } else if (action.equals("console:")) {
            String cmd = action.substring(8).trim();
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (action.equals("op:")) {
            String cmd = action.substring(3).trim();
            boolean op = player.isOp();
            try {
                if (!op) {
                    player.setOp(true);
                }
                player.performCommand(cmd);
            } finally {
                player.setOp(op);
            }
        }
    }

    private static Material parseMaterial(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.STONE;
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
        return Material.STONE;
    }
}
