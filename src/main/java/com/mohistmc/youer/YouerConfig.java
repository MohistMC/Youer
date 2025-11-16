package com.mohistmc.youer;

import com.google.common.base.Throwables;
import com.mohistmc.youer.commands.BackupWorldCommand;
import com.mohistmc.youer.commands.DumpCommand;
import com.mohistmc.youer.commands.HideAllCommand;
import com.mohistmc.youer.commands.HideCommand;
import com.mohistmc.youer.commands.InfoCommand;
import com.mohistmc.youer.commands.ItemsCommand;
import com.mohistmc.youer.commands.PermissionCommand;
import com.mohistmc.youer.commands.ShowsCommand;
import com.mohistmc.youer.commands.YouerCommand;
import com.mohistmc.youer.plugins.MohistPlugin;
import com.mohistmc.youer.plugins.ban.BansCommand;
import com.mohistmc.youer.plugins.menu.MenuCommand;
import com.mohistmc.youer.util.YamlUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class YouerConfig {

    private static final List<String> HEADER = Arrays.asList("""
            This is the main configuration file for Youer.
            As you can see, there's tons to configure. Some options may impact gameplay, so use
            with caution, and make sure you know what each option does before configuring.
            For a reference for any variable inside this file, check out the Youer wiki at
            - [English Documentation](https://mohistmc.com/youer/docs)
            - [中文文档](https://www.mohistmc.cn/docs/youer)
            
            If you need help with the configuration or have any questions related to Spigot,
            join us at the Discord or drop by our forums and leave a post.
            
            Discord: https://discord.gg/mohistmc
            Forums: https://mohistmc.com/
            Forums (中文): https://www.mohistmc.cn/
            
            """.split("\\n"));
    /*========================================================================*/
    public static YamlConfiguration config;
    public static File youeryml = new File("youer-config", "youer.yml");
    public static YamlConfiguration yml = YamlConfiguration.loadConfiguration(youeryml);
    public static boolean show_logo;
    public static String youer_lang;
    public static boolean check_update;
    public static int maximumRepairCost;
    public static boolean enchantment_fix;
    public static int max_enchantment_level;
    public static int maxBees;
    public static boolean bookAnimationTick;
    public static boolean networkmanager_debug;
    public static List<String> networkmanager_intercept;
    public static boolean keepinventory_global;
    public static boolean keepinventory_inventory;
    public static boolean keepinventory_permission_enable;
    public static String keepinventory_inventory_permission;
    public static boolean keepinventory_exp;
    public static String keepinventory_exp_permission;
    // Thread Priority
    public static int server_thread;
    public static boolean clear_item;
    public static List<String> clear_item_whitelist;
    public static String clear_item_msg;
    public static int clear_item_time;
    public static boolean clear_monster;
    public static List<String> clear_monster_whitelist;
    public static String clear_monster_msg;
    public static int clear_monster_time;
    // Ban
    public static boolean ban_item_enable;
    public static boolean ban_block_enable;
    public static boolean ban_entity_enable;
    public static boolean no_vanilla_entity_enable;
    public static List<String> no_vanilla_entity_whitelist;
    public static boolean ban_enchantment_enable;
    public static boolean ban_recipe_enable;
    public static String pingCommandOutput;
    // Ban events
    public static boolean doFireTick;
    public static boolean explosion;
    public static boolean farmlandTrample;
    public static boolean worldmanage;
    public static boolean bukkitpermissionshandler;
    public static boolean recipe_warn;
    public static boolean tpa_enable;
    public static boolean tpa_permissions_enable;
    public static boolean back_enable;
    public static boolean back_permissions_enable;
    public static boolean permissions_debug_console;
    public static boolean permissions_send_player;
    public static boolean watchdog_spigot;
    public static boolean watchdog_mohist;
    public static boolean pluginchannel_debug;
    public static boolean deepseek_enable;
    public static String deepseek_baseUrl;
    public static String deepseek_apikey;
    public static String deepseek_model;
    public static String deepseek_system;
    public static String deepseek_command;
    public static String deepseek_all_command;
    public static String deepseek_chatformat;
    public static boolean warps_enable;
    public static boolean custom_no_villager;
    public static boolean custom_entity_tp_end;
    public static boolean custom_entity_tp_nether;
    public static boolean custom_raid_no_emerald;
    public static int custom_lava_speed_normal;
    public static int custom_lava_speed_nether;
    static int version;
    static Map<String, Command> commands;
    private static File CONFIG_FILE;

    public static void init(File configFile) {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try {
            config.load(CONFIG_FILE);
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load youer.yml, please correct your syntax errors", ex);
            Throwables.throwIfUnchecked(ex);
        }

        config.options().setHeader(HEADER);
        config.options().copyDefaults(true);

        commands = new HashMap<>();
        commands.put("youer", new YouerCommand("youer"));
        commands.put("dump", new DumpCommand("dump"));
        commands.put("backupworld", new BackupWorldCommand("backupworld"));
        commands.put("items", new ItemsCommand("items"));
        commands.put("permission", new PermissionCommand("permission"));
        commands.put("bans", new BansCommand("bans"));
        commands.put("shows", new ShowsCommand("shows"));
        commands.put("infos", new InfoCommand("infos"));
        commands.put("menus", new MenuCommand("menus"));
        commands.put("hideall", new HideAllCommand("hideall"));
        commands.put("hide", new HideCommand("hide"));

        MohistPlugin.registerCommands(commands);

        version = getInt("config-version", 1);
        set("config-version", 1);
        readConfig();

        try {
            Class.forName("org.sqlite.JDBC");
            Class.forName("com.mysql.cj.jdbc.Driver");
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (Throwable t) {
            throw new RuntimeException("Error initializing Youer", t);
        }
    }

    public static void save() {
        YamlUtils.save(youeryml, yml);
    }

    public static void registerCommands() {
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            MinecraftServer.getServer().server.getCommandMap().register(entry.getKey(), "Youer", entry.getValue());
        }
    }

    static void readConfig() {
        for (Method method : YouerConfig.class.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(null);
                    } catch (InvocationTargetException ex) {
                        Throwables.throwIfUnchecked(ex.getCause());
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error invoking " + method, ex);
                    }
                }
            }
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILE, ex);
        }
    }

    private static void set(String path, Object val) {
        config.set(path, val);
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    private static <T> List<String> getStringList(String path, T def) {
        config.addDefault(path, def);
        return config.getStringList(path);
    }

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    private static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    public static String youer_lang() {
        return yml.getString("youer.lang", Locale.getDefault().toString());
    }

    private static void mohist() {
        show_logo = getBoolean("youer.show_logo", true);
        youer_lang = getString("youer.lang", Locale.getDefault().toString());
        check_update = getBoolean("youer.check_update", true);
        watchdog_spigot = getBoolean("youer.watchdog_spigot", true);
        watchdog_mohist = getBoolean("youer.watchdog_mohist", false);
        maximumRepairCost = getInt("anvilfix.maximumrepaircost", 40);
        enchantment_fix = getBoolean("anvilfix.enchantment_fix", false);
        max_enchantment_level = getInt("anvilfix.max_enchantment_level", 32767);
        maxBees = getInt("max-bees-in-hive", 3);
        bookAnimationTick = getBoolean("enchantment-table-book-animation-tick", false);
        networkmanager_debug = getBoolean("networkmanager.debug", false);
        networkmanager_intercept = getStringList("networkmanager.intercept", new ArrayList<>());
        keepinventory_global = getBoolean("keepinventory.global.enable", false);
        keepinventory_permission_enable = getBoolean("keepinventory.permission.enable", false);
        keepinventory_inventory = getBoolean("keepinventory.global.inventory", true);
        keepinventory_inventory_permission = getString("keepinventory.permission.inventory", "youer.keepinventory.inventory");
        keepinventory_exp = getBoolean("keepinventory.global.exp", true);
        keepinventory_exp_permission = getString("keepinventory.permission.exp", "youer.keepinventory.exp");
        server_thread = getInt("threadpriority.server_thread", 8);

        clear_item = getBoolean("entity.clear.item.enable", false);
        clear_item_whitelist = getStringList("entity.clear.item.whitelist", new ArrayList<>());
        clear_item_msg = getString("entity.clear.item.msg", "[Server] Cleaned up %size% drop item");
        clear_item_time = getInt("entity.clear.item.time", 1800);

        clear_monster = getBoolean("entity.clear.monster.enable", false);
        clear_monster_whitelist = getStringList("entity.clear.monster.whitelist", new ArrayList<>());
        clear_monster_msg = getString("entity.clear.monster.msg", "[Server] Cleaned up %size% monster");
        clear_monster_time = getInt("entity.clear.monster.time", 1800);

        ban_item_enable = getBoolean("bans.item", false);
        ban_block_enable = getBoolean("bans.block", false);
        ban_entity_enable = getBoolean("bans.entity.enable", false);
        no_vanilla_entity_enable = getBoolean("bans.entity.vanilla_entity.enable", false);
        no_vanilla_entity_whitelist = getStringList("bans.entity.vanilla_entity.whitelist", new ArrayList<>());
        ban_enchantment_enable = getBoolean("bans.enchantment", false);
        ban_recipe_enable = getBoolean("bans.recipe", false);

        pingCommandOutput = getString("settings.messages.ping-command-output", "§2%s's ping is %sms");

        doFireTick = getBoolean("events.fire_tick", false);
        explosion = getBoolean("events.explosion", false);
        farmlandTrample = getBoolean("events.farmlandTrample", false);
        bukkitpermissionshandler = getBoolean("neoforge.bukkitpermissionshandler", true);
        worldmanage = getBoolean("worldmanage", true);

        recipe_warn = getBoolean("recipe.warn", false);
        tpa_enable = getBoolean("tpa.enable", false);
        tpa_permissions_enable = getBoolean("tpa.permissions", true);
        back_enable = getBoolean("back.enable", false);
        back_permissions_enable = getBoolean("back.permissions", true);

        permissions_debug_console = getBoolean("permissions.debug.console", false);
        permissions_send_player = getBoolean("permissions.debug.player", false);
        pluginchannel_debug = getBoolean("pluginchannel.debug", false);

        deepseek_enable = getBoolean("deepseek.enable", false);
        deepseek_baseUrl = getString("deepseek.baseUrl", "https://api.deepseek.com/chat/completions");
        deepseek_apikey = getString("deepseek.apikey", "youer");
        deepseek_model = getString("deepseek.model", "deepseek-chat");
        deepseek_system = getString("deepseek.system", "你的名字叫小小墨，年龄18岁，是个可爱的女孩子!");
        deepseek_command = getString("deepseek.command", "ai");
        deepseek_all_command = getString("deepseek.all_command", "ai-all");
        deepseek_chatformat = getString("deepseek.chatformat", "<小小墨> %s");

        warps_enable = getBoolean("warps.enable", false);

        custom_no_villager = getBoolean("custom.no_villager", false);
        custom_entity_tp_end = getBoolean("custom.entity_tp_end", true);
        custom_entity_tp_nether = getBoolean("custom.entity_tp_nether", true);
        custom_raid_no_emerald = getBoolean("custom.raid_no_emerald", false);
        custom_lava_speed_normal = getInt("custom.lava_speed.normal", 30);
        custom_lava_speed_nether = getInt("custom.lava_speed.nether", 10);

        getBoolean("keepinventory.world.inventory", false);
        getBoolean("keepinventory.world.exp", false);
    }

    public static boolean isCN() {
        return yml.getString("youer.lang", Locale.getDefault().toString()).contains("CN");
    }
}