package com.mohistmc.youer.ai.tool.command;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolHandler;
import com.mohistmc.youer.api.ai.tool.AiToolResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ConsoleCommandTool implements AiToolHandler {
    private final AiCommandGateway gateway;
    private final AiCommandSanitizer sanitizer;
    public ConsoleCommandTool(AiCommandGateway gateway, AiCommandSanitizer sanitizer) {
        this.gateway = gateway; this.sanitizer = sanitizer;
    }
    @Override public CompletionStage<AiToolResult> execute(AiToolContext context, Json arguments) {
        String command = sanitizer.normalize(arguments.at("command").asString());
        return CompletableFuture.completedFuture(gateway.dispatchConsole(command)
                ? AiToolResult.success("Console command dispatched")
                : AiToolResult.error("Console command was not dispatched"));
    }
}
