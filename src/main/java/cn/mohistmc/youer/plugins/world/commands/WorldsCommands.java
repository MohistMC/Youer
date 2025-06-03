package cn.mohistmc.youer.plugins.world.commands;

import cn.mohistmc.youer.api.ItemAPI;
import cn.mohistmc.youer.neoforge.NeoForgeInjectBukkit;
import cn.mohistmc.youer.plugins.world.WorldManage;
import cn.mohistmc.youer.plugins.world.listener.InventoryClickListener;
import cn.mohistmc.youer.plugins.world.utils.ConfigByWorlds;
import cn.mohistmc.youer.plugins.world.utils.WorldInventory;
import cn.mohistmc.youer.plugins.world.utils.WorldInventoryType;
import cn.mohistmc.youer.plugins.world.utils.WorldsGUI;
import cn.mohistmc.youer.util.I18n;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WorldsCommands extends Command {
    public static String type;

    public WorldsCommands(String name) {
        super(name);
        this.description = "World Manager.";
        this.usageMessage = "/worlds";
        this.setPermission("youer.command.worlds");
    }

    public static void worldNotExists(Player player, String world) {
        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.command.thisWorld") + world + I18n.as("worldcommands.command.worldDontExist"));
    }

    public static void worldAllExists(Player player, String world) {
        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.command.thisWorld") + world + I18n.as("worldcommands.command.worldExists"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return false;
        }
        if (sender instanceof Player player) {
            World worldByplayer = player.getWorld();
            if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
                WorldsGUI.openWorldGui(player, I18n.as("worldmanage.gui.title1"));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("addtoconfig")) {
                ConfigByWorlds.addWorld(worldByplayer.getName(), false);
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                type = args[1].toLowerCase(java.util.Locale.ENGLISH);
                if (Bukkit.getWorld(args[1]) == null) {
                    int i = -1;
                    WorldInventory worldCreateInventory = new WorldInventory(
                            WorldInventoryType.CREATE,
                            27,
                            I18n.as("worldmanage.gui.title0") + type);
                    Inventory inventory = worldCreateInventory.getInventory();
                    for (World.Environment environment : NeoForgeInjectBukkit.environment.values()) {
                        if (environment == World.Environment.CUSTOM) continue;
                        i++;
                        inventory.setItem(i, ItemAPI.doItem(Material.MAP, 1, environment.name(), null));
                    }
                    inventory.addItem(ItemAPI.doItem(Material.MAP, 1, "void", null));
                    player.openInventory(inventory);
                    InventoryClickListener.worldInventory = worldCreateInventory;
                    return true;
                } else {
                    worldAllExists(player, type);
                    return false;
                }
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
                String worldName = args[1];
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    worldNotExists(player, worldName);
                    return false;
                }
                ConfigByWorlds.getSpawn(worldName, player);
                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.command.teleport") + worldName + I18n.as("worldcommands.command.spawn"));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("spawn")) {
                ConfigByWorlds.getSpawn(worldByplayer.getName(), player);
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
                String worldName = args[1];
                if (!args[1].equalsIgnoreCase("world")) {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        for (Player all : w.getPlayers()) {
                            all.teleport(MinecraftServer.getServer().overworld().world.getSpawnLocation()); // use overworld
                        }
                        try {
                            ConfigByWorlds.removeWorld(worldName);
                            Bukkit.unloadWorld(w, true);
                            File deleteWorld = w.getWorldFolder();
                            WorldManage.deleteDir(deleteWorld);
                            player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.delSuccessful"));
                            return true;
                        } catch (Exception e2) {
                            player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.delUnsuccessful"));
                            return false;
                        }
                    }
                } else {
                    player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.delDenied"));
                    return false;
                }
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
                String worldName = args[1].toLowerCase(java.util.Locale.ENGLISH);
                try {
                    World w = Bukkit.getWorld(worldName);
                    player.teleport(w.getSpawnLocation());
                    player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldExistsTele"));
                    return true;
                } catch (Exception e3) {
                    File loadWorld = new File(worldName); // TODO forge and bukkit world file path?
                    if (loadWorld.exists()) {
                        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.loadworld"));
                        World w = Bukkit.createWorld(new WorldCreator(worldName));
                        if (w != null) {
                            Location location = w.getSpawnLocation();
                            player.teleport(location);
                            ConfigByWorlds.addSpawn(location);
                        }
                        ConfigByWorlds.addWorld(worldName, true);
                        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.loadWorldSuccessful"));
                        return true;
                    } else {
                        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldFileNotfound"));
                        return false;
                    }
                }
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("unload")) {
                String worldName = args[1];
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    return false;
                }
                for (Player all2 : world.getPlayers()) {
                    all2.teleport(Bukkit.getWorld("world").getSpawnLocation());
                }
                Bukkit.unloadWorld(world, true);
                ConfigByWorlds.removeWorld(worldName);
                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldUnload"));
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("addinfo")) {
                ConfigByWorlds.addInfo(worldByplayer.getName(), args[1]);
                player.sendMessage(I18n.as("worldmanage.prefix") +  I18n.as("worldcommands.world.worldSetupSuccess"));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.info", worldByplayer.getName()));
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("setname")) {
                String worldname = worldByplayer.getName();
                ConfigByWorlds.addname(worldname, args[1]);
                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("setspawn")) {
                ConfigByWorlds.addSpawn(player.getLocation());
                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("difficulty")) {
                Integer difficulty = Integer.valueOf(args[1]);
                if (difficulty != null) {
                    if (difficulty >= 0 && difficulty < 4) {
                        switch (difficulty) {
                            case 0 -> {
                                player.getWorld().setDifficulty(Difficulty.PEACEFUL);
                                ConfigByWorlds.setnandu(player, "PEACEFUL");
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                            case 1 -> {
                                player.getWorld().setDifficulty(Difficulty.EASY);
                                ConfigByWorlds.setnandu(player, "EASY");
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                            case 2 -> {
                                player.getWorld().setDifficulty(Difficulty.NORMAL);
                                ConfigByWorlds.setnandu(player, "NORMAL");
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                            case 3 -> {
                                player.getWorld().setDifficulty(Difficulty.HARD);
                                ConfigByWorlds.setnandu(player, "HARD");
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                        }
                        return true;
                    } else {
                        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.setDifFailure"));
                    }
                } else {
                    player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.setDifFailure"));
                }
                return false;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("gamemode")) {
                Integer gamemode = Integer.valueOf(args[1]);
                if (gamemode != null) {
                    if (gamemode >= 0 && gamemode < 4) {
                        switch (gamemode) {
                            case 0 -> {
                                WorldManage.changeGameMode(player.getWorld(), GameMode.SURVIVAL);
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                            case 1 -> {
                                WorldManage.changeGameMode(player.getWorld(), GameMode.CREATIVE);
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                            case 2 -> {
                                WorldManage.changeGameMode(player.getWorld(), GameMode.ADVENTURE);
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                            case 3 -> {
                                WorldManage.changeGameMode(player.getWorld(), GameMode.SPECTATOR);
                                player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.worldSetupSuccess"));
                            }
                        }
                        return true;
                    } else {
                        player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.setDifFailure"));
                    }
                } else {
                    player.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.setDifFailure"));
                }
                return false;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("cleardropitem")) {
                AtomicInteger size = new AtomicInteger(0);
                for (org.bukkit.entity.Entity entity : worldByplayer.getEntities().stream().toList()) {
                    if (entity.getType() == EntityType.ITEM) {
                        ItemStack item = ((org.bukkit.entity.Item) entity).getItemStack();
                        entity.remove();
                        size.addAndGet(item.getAmount());
                    }
                }
                sender.sendMessage(I18n.as("worldmanage.prefix") + I18n.as("worldcommands.world.cleardropitem", size.get(), worldByplayer.getName()));
                return true;

            }
        } else {
            if (args.length == 3 && args[0].equalsIgnoreCase("tp") && sender.isOp()) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    String name = target.getName();
                    String argsname = args[1];
                    Player target1 = Bukkit.getPlayer(name);
                    if (target1 == null) {
                        return false;
                    }
                    if (argsname.equals(name)) {
                        String worldName = args[2];
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            return false;
                        }
                        ConfigByWorlds.getSpawn(worldName, target1);
                        return true;
                    }
                }
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                List<String> c = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) {
                    c.add(world.getName());
                }
                sender.sendMessage(I18n.as("worldmanage.prefix") + c);
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) throws IllegalArgumentException {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            list.add("info");
        }

        if (args.length >= 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("delete")) || args[0].equalsIgnoreCase("unload")) {
            list.addAll(Bukkit.getWorldsByName());
        }

        return list;
    }

    private final List<String> params = Arrays.asList("create", "delete", "tp", "import", "unload", "info", "addinfo", "setname", "setspawn", "gui", "difficulty", "cleardropitem", "gamemode");


    private void sendHelp(CommandSender player) {
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds create <Name> " + I18n.as("worldmanage.command.create"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds delete <Name> " + I18n.as("worldmanage.command.delete"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds tp <Name> " + I18n.as("worldmanage.command.tp"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds tp <Player> <Name> " + I18n.as("worldmanage.command.tp0"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds import <Name> " + I18n.as("worldmanage.command.import"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds unload <Name> " + I18n.as("worldmanage.command.unload"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds info " + I18n.as("worldmanage.command.info"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds addinfo <Name> " + I18n.as("worldmanage.command.addinfo"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds setname <Name> " + I18n.as("worldmanage.command.setname"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds setspawn " + I18n.as("worldmanage.command.setspawn"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds gui " + I18n.as("worldmanage.command.gui"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds difficulty <0-3> " + I18n.as("worldmanage.command.difficulty"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds cleardropitem " + I18n.as("worldmanage.command.cleardropitem"));
        player.sendMessage(I18n.as("worldmanage.prefix") + "/worlds gamemode <0-3>" + I18n.as("worldmanage.command.gamemode"));
    }
}
