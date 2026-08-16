package com.mohistmc.youer.commands;

import com.mohistmc.youer.feature.enchantlimit.EnchantLimitConfig;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

public class EnchantLimitCommand extends Command {

    private static final List<String> SUBCOMMANDS = List.of("add", "list", "remove");

    public EnchantLimitCommand(String name) {
        super(name);
        this.description = I18n.as("enchantlimitcmd.description");
        this.setPermission("youer.command.enchantlimit");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return false;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "add" -> add(sender, args);
            case "list" -> list(sender);
            case "remove" -> remove(sender, args);
            default -> {
                sendHelp(sender);
                return false;
            }
        }
        return true;
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + I18n.as("enchantlimitcmd.add.usage"));
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(args[1]);
        if (key == null || Enchantment.getByKey(key) == null) {
            sender.sendMessage(ChatColor.RED + I18n.as("enchantlimitcmd.add.notexists").formatted(args[1]));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + I18n.as("enchantlimitcmd.add.invalidnumber"));
            return;
        }
        if (level <= 0) {
            sender.sendMessage(ChatColor.RED + I18n.as("enchantlimitcmd.add.invalidnumber"));
            return;
        }
        EnchantLimitConfig.setLimit(key.asString(), level);
        sender.sendMessage(ChatColor.GREEN + I18n.as("enchantlimitcmd.add.success").formatted(key, level));
    }

    private void list(CommandSender sender) {
        Map<String, Integer> limits = EnchantLimitConfig.getLimits();
        if (limits.isEmpty()) {
            sender.sendMessage(I18n.as("enchantlimitcmd.list.empty"));
            return;
        }
        sender.sendMessage(I18n.as("enchantlimitcmd.list.header"));
        int i = 1;
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            sender.sendMessage(I18n.as("enchantlimitcmd.list.entry", i++, entry.getKey(), entry.getValue()));
        }
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + I18n.as("enchantlimitcmd.remove.usage"));
            return;
        }
        String name = args[1];
        if (!EnchantLimitConfig.getLimits().containsKey(name)) {
            sender.sendMessage(ChatColor.RED + I18n.as("enchantlimitcmd.remove.notexists").formatted(name));
            return;
        }
        EnchantLimitConfig.removeLimit(name);
        sender.sendMessage(ChatColor.GREEN + I18n.as("enchantlimitcmd.remove.success").formatted(name));
    }

    private void sendHelp(CommandSender sender) {
        for (String line : I18n.as("enchantlimitcmd.help").split("\n")) {
            sender.sendMessage(line);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String cmd : SUBCOMMANDS) {
                if (cmd.startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            for (Enchantment enchantment : Enchantment.values()) {
                String name = enchantment.getKey().asString();
                if (name.toLowerCase(Locale.ENGLISH).startsWith(args[1].toLowerCase(Locale.ENGLISH))) {
                    completions.add(name);
                }
            }
        }
        return completions;
    }
}
