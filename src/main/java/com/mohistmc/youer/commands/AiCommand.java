package com.mohistmc.youer.commands;

import com.mohistmc.youer.ai.AiChatHandler;
import com.mohistmc.youer.ai.AiChatService;
import com.mohistmc.youer.ai.history.AiConversationSnapshot;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AiCommand extends BukkitCommand {

    private static final List<String> PARAMETERS = List.of("history", "clearall", "clear", "tools", "confirm", "cancel");

    public AiCommand(String name) {
        super(name);
        this.description = "AI chat administration";
        this.usageMessage = "/ai <clear|clearall|history|tools|confirm|cancel>";
        this.setPermission("youer.command.ai;youer.ai.tools.use");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        boolean administrativePermission = sender.hasPermission("youer.command.ai.admin");
        String subcommand = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
        if (!mayUseSubcommand(sender, subcommand)) {
            sender.sendMessage(I18n.as("ai.command.no_permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.as("hatcmd.not.player"));
            return true;
        }
        AiChatService service = AiChatHandler.service();
        if (service == null) {
            sender.sendMessage(I18n.as("ai.unavailable"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(I18n.as("ai.command.usage"));
            return true;
        }

        switch (subcommand) {
            case "confirm" -> sender.sendMessage(args.length == 2 && AiChatHandler.confirm(player.getUniqueId(), args[1])
                    ? I18n.as("ai.command.confirm.success") : I18n.as("ai.command.pending.notfound"));
            case "cancel" -> sender.sendMessage(args.length == 2 && AiChatHandler.cancel(player.getUniqueId(), args[1])
                    ? I18n.as("ai.command.cancel.success") : I18n.as("ai.command.pending.notfound"));
            case "tools" -> sender.sendMessage(I18n.as("ai.command.tools", AiChatHandler.tools(player).definitions().stream()
                    .map(definition -> definition.name() + " ["
                            + I18n.as("ai.tool.risk." + definition.risk().name().toLowerCase(java.util.Locale.ROOT))
                            + "; " + definition.permission() + "]")
                    .collect(java.util.stream.Collectors.joining(", "))));
            case "clear" -> clear(sender, player, service, args, administrativePermission);
            case "clearall" -> {
                if (!administrativePermission) {
                    sender.sendMessage(I18n.as("ai.command.no_permission"));
                } else {
                    service.clearAll();
                    sender.sendMessage(I18n.as("ai.command.clearall.success"));
                }
            }
            case "history" -> showHistory(player, service);
            default -> sender.sendMessage(I18n.as("ai.command.usage"));
        }
        return true;
    }

    static boolean mayUseSubcommand(CommandSender sender, String subcommand) {
        if (sender.hasPermission("youer.command.ai")) return true;
        return ("confirm".equals(subcommand) || "cancel".equals(subcommand) || "tools".equals(subcommand))
                && sender.hasPermission("youer.ai.use")
                && sender.hasPermission("youer.ai.tools.use");
    }

    private static void clear(
            CommandSender sender,
            Player player,
            AiChatService service,
            String[] args,
            boolean administrativePermission) {
        UUID targetId = player.getUniqueId();
        if (args.length > 1) {
            if (!administrativePermission) {
                sender.sendMessage(I18n.as("ai.command.no_permission"));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(I18n.as("ai.command.player.notfound", args[1]));
                return;
            }
            targetId = target.getUniqueId();
        }
        service.clear(targetId);
        sender.sendMessage(I18n.as("ai.command.clear.success"));
    }

    private static void showHistory(Player player, AiChatService service) {
        Map<UUID, AiConversationSnapshot> histories = service.histories();
        String model = service.runtime().profile().model();
        DemoGUI gui = new DemoGUI(I18n.as("ai.history.title", model, histories.size()));
        histories.forEach((playerId, snapshot) -> gui.addItem(new GUIItem(new ItemStackFactory(Material.PLAYER_HEAD)
                .setLore(List.of(I18n.as("ai.history.lore", snapshot.messages().size())))
                .player(Bukkit.getPlayer(playerId))
                .buildHead())));
        gui.openGUI(player);
    }

    @Override
    public @NotNull List<String> tabComplete(
            @NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> matches = new ArrayList<>();
        if (args.length == 1) {
            for (String parameter : PARAMETERS) {
                if (parameter.startsWith(args[0].toLowerCase())
                        && mayUseSubcommand(sender, parameter)) {
                    matches.add(parameter);
                }
            }
        } else if (args.length == 2 && "clear".equalsIgnoreCase(args[0])
                && mayUseSubcommand(sender, "clear")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    matches.add(player.getName());
                }
            }
        }
        return matches;
    }
}
