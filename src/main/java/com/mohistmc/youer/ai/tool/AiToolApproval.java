package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface AiToolApproval {
    CompletionStage<AiToolApprovalDecision> request(
            AiToolContext context, AiRegisteredTool tool, AiToolCallContent call);
}
