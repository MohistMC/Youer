package com.mohistmc.youer.commands;

import com.mohistmc.youer.feature.entitydamage.EntityDamageConfig;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EntityDamageCommand extends Command {

    private static final List<String> SUBCOMMANDS = List.of("add", "list", "remove");

    public EntityDamageCommand(String name) {
        super(name);
        this.description = I18n.as("entitydamagecmd.description");
        this.setPermission("youer.command.entitydamage");
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
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.add.usage"));
            return;
        }
        ResourceLocation key;
        try {
            key = ResourceLocation.parse(args[1]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.add.notexists").formatted(args[1]));
            return;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.add.notexists").formatted(args[1]));
            return;
        }
        int percent;
        try {
            percent = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.add.invalidpercent"));
            return;
        }
        if (percent <= 0 || percent > 100) {
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.add.invalidpercent"));
            return;
        }
        EntityDamageConfig.setWeaken(key.toString(), percent);
        sender.sendMessage(ChatColor.GREEN + I18n.as("entitydamagecmd.add.success").formatted(key, percent));
    }

    private void list(CommandSender sender) {
        Map<String, Integer> weaken = EntityDamageConfig.getWeaken();
        if (weaken.isEmpty()) {
            sender.sendMessage(I18n.as("entitydamagecmd.list.empty"));
            return;
        }
        sender.sendMessage(I18n.as("entitydamagecmd.list.header"));
        int i = 1;
        for (Map.Entry<String, Integer> entry : weaken.entrySet()) {
            sender.sendMessage(I18n.as("entitydamagecmd.list.entry", i++, entry.getKey(), entry.getValue()));
        }
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.remove.usage"));
            return;
        }
        String name = args[1];
        if (!EntityDamageConfig.getWeaken().containsKey(name)) {
            sender.sendMessage(ChatColor.RED + I18n.as("entitydamagecmd.remove.notexists").formatted(name));
            return;
        }
        EntityDamageConfig.removeWeaken(name);
        sender.sendMessage(ChatColor.GREEN + I18n.as("entitydamagecmd.remove.success").formatted(name));
    }

    private void sendHelp(CommandSender sender) {
        for (String line : I18n.as("entitydamagecmd.help").split("\n")) {
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
            for (ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                String name = key.toString();
                if (name.toLowerCase(Locale.ENGLISH).startsWith(args[1].toLowerCase(Locale.ENGLISH))) {
                    completions.add(name);
                }
            }
        }
        return completions;
    }
}
