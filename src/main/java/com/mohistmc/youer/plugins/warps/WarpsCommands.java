package com.mohistmc.youer.plugins.warps;

import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul by MohistMC
 * @date 2023/9/12 16:27:32
 */
public class WarpsCommands extends Command {

    private final List<String> params = Arrays.asList("set", "del", "tp", "gui");

    public WarpsCommands(String name) {
        super(name);
        this.description = "Warps Manager.";
        this.usageMessage = "/warps";
        this.setPermission("youer.command.warps");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return false;
        }
        if (sender instanceof Player player) {
            if (args.length == 2) {
                switch (args[0].toLowerCase(Locale.ENGLISH)) {
                    case "set" -> {
                        String name = args[1];
                        WarpsConfig.INSTANCE.put(name, player.getLocation());
                        player.sendMessage(I18n.as("warpscommands.set.success", name));
                        return true;
                    }
                    case "del" -> {
                        String name = args[1];
                        if (WarpsConfig.INSTANCE.has(name)) {
                            WarpsConfig.INSTANCE.remove(name);
                            player.sendMessage(I18n.as("warpscommands.nowarp"));
                            return true;
                        } else {
                            player.sendMessage(I18n.as("warpscommands.del.success", name));
                            return false;
                        }
                    }
                    case "tp" -> {
                        String name = args[1];
                        if (WarpsConfig.INSTANCE.has(name)) {
                            player.teleport(WarpsConfig.INSTANCE.get(name));
                            return true;
                        } else {
                            player.sendMessage(I18n.as("warpscommands.nowarp"));
                            return false;
                        }
                    }
                }
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
                DemoGUI wh = new DemoGUI(I18n.as("warpscommands.prefix"));
                for (String w : WarpsConfig.INSTANCE.yaml.getKeys(false)) {
                    wh.addItem(new GUIItem(new ItemStackFactory(Material.BAMBOO_SIGN)
                            .setDisplayName(w)
                            .setLore(List.of(I18n.as("warpscommands.gui.click"), "§f" + WarpsConfig.INSTANCE.get(w).asString()))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            u.teleport(WarpsConfig.INSTANCE.get(w));
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
            String playerName = args[1];
            String warpsName = args[2];
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sender.sendMessage(I18n.as("warpscommands.noplayer"));
                return false;
            }
            if (WarpsConfig.INSTANCE.has(warpsName)) {
                player.teleport(WarpsConfig.INSTANCE.get(warpsName));
                return true;
            } else {
                sender.sendMessage(I18n.as("warpscommands.nowarp"));
                return false;
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
        return list;
    }

    private void sendHelp(CommandSender player) {
        String prefix = I18n.as("warpscommands.prefix");
        player.sendMessage("§6§l" + prefix);
        player.sendMessage("§a/warps set §b<Name> §7- " + I18n.as("warpscommands.set"));
        player.sendMessage("§a/warps del §b<Name> §7- " + I18n.as("warpscommands.del"));
        player.sendMessage("§a/warps tp §b<Name> §7- " + I18n.as("warpscommands.tp"));
        player.sendMessage("§a/warps tp §b<Player> <Name> §7- " + I18n.as("warpscommands.tp0"));
        player.sendMessage("§a/warps gui §7- " + I18n.as("warpscommands.gui"));
    }
}
