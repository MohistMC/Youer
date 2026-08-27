package com.mohistmc.youer.ai.tool.command;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolHandler;
import com.mohistmc.youer.api.ai.tool.AiToolResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SearchCommandsTool implements AiToolHandler {
    private final AiCommandGateway gateway;
    private final Function<UUID, Player> players;

    public SearchCommandsTool(AiCommandGateway gateway, Function<UUID, Player> players) {
        this.gateway = gateway;
        this.players = players;
    }

    @Override public CompletionStage<AiToolResult> execute(AiToolContext context, Json arguments) {
        Player player = players.apply(context.playerId());
        if (player == null) return CompletableFuture.completedFuture(AiToolResult.error("Player is offline"));
        String mode = arguments.has("mode") ? arguments.at("mode").asString() : "player";
        CommandSender sender = player;
        if ("console".equalsIgnoreCase(mode)) {
            if (!player.hasPermission("youer.ai.tools.command.console")) {
                return CompletableFuture.completedFuture(AiToolResult.error("Permission denied"));
            }
            sender = Bukkit.getConsoleSender();
        }
        String query = arguments.has("query") ? arguments.at("query").asString() : "";
        return CompletableFuture.completedFuture(AiToolResult.success(
                gateway.search(sender, query, 25).toString()));
    }
}
