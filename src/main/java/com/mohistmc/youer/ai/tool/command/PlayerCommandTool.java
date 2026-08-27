package com.mohistmc.youer.ai.tool.command;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolHandler;
import com.mohistmc.youer.api.ai.tool.AiToolResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.bukkit.entity.Player;

public final class PlayerCommandTool implements AiToolHandler {
    private final AiCommandGateway gateway;
    private final AiCommandSanitizer sanitizer;
    private final Function<UUID, Player> players;

    public PlayerCommandTool(AiCommandGateway gateway, AiCommandSanitizer sanitizer, Function<UUID, Player> players) {
        this.gateway = gateway; this.sanitizer = sanitizer; this.players = players;
    }
    @Override public CompletionStage<AiToolResult> execute(AiToolContext context, Json arguments) {
        Player player = players.apply(context.playerId());
        if (player == null) return CompletableFuture.completedFuture(AiToolResult.error("Player is offline"));
        String command = sanitizer.normalize(arguments.at("command").asString());
        return CompletableFuture.completedFuture(gateway.dispatchPlayer(player, command)
                ? AiToolResult.success("Command dispatched") : AiToolResult.error("Command was not dispatched"));
    }
}
