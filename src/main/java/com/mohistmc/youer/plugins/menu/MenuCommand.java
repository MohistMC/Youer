package com.mohistmc.youer.plugins.menu;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class MenuCommand extends Command {

    private final List<String> params = List.of("open");

    public MenuCommand(String name) {
        super(name);
        this.description = "Menu command";
        this.usageMessage = "/menus [open] [name]";
        this.setPermission("youer.command.menus");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        } else if (args.length == 2 && "open".equalsIgnoreCase(args[0]) && (sender.isOp() || testPermission(sender))) {
            File menuFolder = new File("youer-config/menu");
            if (menuFolder.exists() && menuFolder.isDirectory()) {
                File[] files = menuFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.toLowerCase().endsWith(".yml")) {
                            fileName = fileName.substring(0, fileName.length() - 4);
                        }

                        if (fileName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            list.add(fileName);
                        }
                    }
                }
            }
        }

        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("open")) {
            sender.sendMessage("/menus open <nmenu_name>");
            return true;
        }

        String fileName = args[1];

        try {
            MenuConfig menuConfig = loadMenuConfig(fileName);
            if (menuConfig != null) {
                MenuGUI.openMenu(player, menuConfig);
            }
        } catch (Exception e) {
            player.sendMessage("Error loading menu: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private MenuConfig loadMenuConfig(String fileName) {
        try {
            File menuFolder = new File("youer-config/menu");
            File menuFile = new File(menuFolder, fileName + ".yml");

            if (!menuFile.exists()) {
                menuFile = new File(menuFolder, fileName);
                if (!menuFile.exists()) {
                    return null;
                }
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

            MenuConfig menuConfig = new MenuConfig();
            menuConfig.setIcons(new HashMap<>());

            if (config.contains("menu-settings")) {
                MenuSettings menuSettings = parseMenuSettings(config);
                menuConfig.setMenuSettings(menuSettings);
            }

            Map<String, Icon> icons = parseIcons(config);
            menuConfig.getIcons().putAll(icons);

            return menuConfig;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private MenuSettings parseMenuSettings(YamlConfiguration config) {
        MenuSettings settings = new MenuSettings();

        settings.setName(config.getString("menu-settings.name"));
        settings.setRows(config.getInt("menu-settings.rows", 0));
        settings.setAutoRefresh(config.contains("menu-settings.auto-refresh") ?
                config.getInt("menu-settings.auto-refresh") : null);
        settings.setOpenActions(config.getStringList("menu-settings.open-actions"));

        return settings;
    }

    private Map<String, Icon> parseIcons(YamlConfiguration config) {
        Map<String, Icon> icons = new HashMap<>();

        Set<String> keys = config.getKeys(false);

        for (String key : keys) {
            if ("menu-settings".equals(key)) {
                continue;
            }

            if (config.isConfigurationSection(key)) {
                Icon icon = new Icon();

                icon.setMaterial(config.getString(key + ".MATERIAL"));
                icon.setPositionX(config.getInt(key + ".POSITION-X", 0));
                icon.setPositionY(config.getInt(key + ".POSITION-Y", 0));
                icon.setName(config.getString(key + ".NAME"));
                icon.setLore(config.getStringList(key + ".LORE"));
                icon.setActions(config.getStringList(key + ".ACTIONS"));
                icon.setDurability(config.contains(key + ".DURABILITY") ?
                        config.getInt(key + ".DURABILITY") : null);
                icon.setEnchantments(config.getStringList(key + ".ENCHANTMENTS"));
                icon.setAmount(config.contains(key + ".AMOUNT") ?
                        config.getInt(key + ".AMOUNT") : null);
                icon.setRequiredItems(config.getStringList(key + ".REQUIRED-ITEMS"));
                icon.setKeepOpen(config.getBoolean(key + ".KEEP-OPEN", false));
                icon.setPermission(config.getString(key + ".PERMISSION"));
                icon.setPermissionMessage(config.getString(key + ".PERMISSION-MESSAGE"));
                icon.setCustomModelData(config.contains(key + ".CUSTOMMODELDATA") ?
                        config.getInt(key + ".CUSTOMMODELDATA") : null);
                icon.setHideTooltip(config.getBoolean(key + ".HIDE-TOOLTIP", false));
                icon.setItemFlags(config.getStringList(key + ".ITEMFLAG").stream()
                        .map(ItemFlag::valueOf).collect(Collectors.toList()));
                icon.setBase64(config.getString(key + ".BASE64"));
                icons.put(key, icon);
            }
        }

        return icons;
    }
}
