package com.mohistmc.youer.api.ai.tool;

import com.mohistmc.mjson.Json;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface AiToolHandler {

    CompletionStage<AiToolResult> execute(AiToolContext context, Json arguments);
}
