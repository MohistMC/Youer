package com.mohistmc.youer.commands;

import com.mohistmc.youer.ai.deepseek.ChatRequest;
import com.mohistmc.youer.ai.deepseek.DeepSeek;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2026/01/07 01:30
 */
public class DeepseekCommand extends BukkitCommand {

    public DeepseekCommand(String name) {
        super(name);
        this.description = "ai chat";
        this.usageMessage = "/deepseek";
        this.setPermission("youer.command.deepseek");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.as("hatcmd.not.player"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(I18n.as("deepseek.command.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "clear" -> {
                UUID targetUUID = player.getUniqueId();
                if (args.length > 1) {
                    Player targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer != null) {
                        targetUUID = targetPlayer.getUniqueId();
                    } else {
                        sender.sendMessage(I18n.as("deepseek.command.player.notfound", args[1]));
                        return true;
                    }
                }
                DeepSeek.clearHistory(targetUUID);
                sender.sendMessage(I18n.as("deepseek.command.clear.success"));
            }
            case "clearall" -> {
                DeepSeek.clearAllHistory();
                sender.sendMessage(I18n.as("deepseek.command.clearall.success"));
            }
            case "history" -> {
                DemoGUI wh = new DemoGUI(I18n.as("deepseek.history.title", DeepSeek.getConversationHistory().size()));
                for (Map.Entry<UUID, List<ChatRequest.Message>> msg : DeepSeek.getConversationHistory().entrySet()) {
                    wh.addItem(new GUIItem(new ItemStackFactory(Material.PLAYER_HEAD)
                            .setLore(List.of(I18n.as("deepseek.history.lore", DeepSeek.getHistorySize(msg.getKey()))))
                            .player(Bukkit.getPlayer(msg.getKey()))
                            .buildHead()));
                }
                wh.openGUI(player);
            }
            default -> sender.sendMessage(I18n.as("deepseek.command.usage"));
        }
        return true;
    }

    private final List<String> params = Arrays.asList("history", "clearall", "clear");

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        } else if (args.length == 2 && "clear".equalsIgnoreCase(args[0])) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                String playerName = onlinePlayer.getName();
                if (playerName.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(playerName);
                }
            }
        }

        return list;
    }

}