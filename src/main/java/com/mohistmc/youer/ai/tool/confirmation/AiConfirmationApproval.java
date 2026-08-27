package com.mohistmc.youer.ai.tool.confirmation;

import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.ai.tool.AiRegisteredTool;
import com.mohistmc.youer.ai.tool.AiToolApproval;
import com.mohistmc.youer.ai.tool.AiToolApprovalDecision;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolRisk;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class AiConfirmationApproval implements AiToolApproval {
    private final AiConfirmationStore store;
    private final Duration timeout;
    private final boolean confirmPlayerCommands;
    private final Consumer<AiPendingAction> notifier;

    public AiConfirmationApproval(AiConfirmationStore store, Duration timeout, boolean confirmPlayerCommands,
            Consumer<AiPendingAction> notifier) {
        this.store = store; this.timeout = timeout; this.confirmPlayerCommands = confirmPlayerCommands;
        this.notifier = notifier;
    }

    @Override public CompletionStage<AiToolApprovalDecision> request(
            AiToolContext context, AiRegisteredTool tool, AiToolCallContent call) {
        AiToolRisk risk = tool.definition().risk();
        if (risk == AiToolRisk.READ_ONLY
                || (risk == AiToolRisk.PLAYER_ACTION && !confirmPlayerCommands)) {
            return java.util.concurrent.CompletableFuture.completedFuture(AiToolApprovalDecision.APPROVED);
        }
        String arguments = call.arguments().toString();
        if (arguments.length() > 512) arguments = arguments.substring(0, 512);
        AiPendingAction action = store.create(context.playerId(), tool.definition().name() + " " + arguments,
                timeout);
        notifier.accept(action);
        return action.decision().thenApply(decision -> switch (decision) {
            case APPROVED -> AiToolApprovalDecision.APPROVED;
            case EXPIRED -> AiToolApprovalDecision.EXPIRED;
            case CANCELLED, REPLACED -> AiToolApprovalDecision.CANCELLED;
            case DENIED -> AiToolApprovalDecision.DENIED;
        });
    }
}
