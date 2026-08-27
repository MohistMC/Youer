package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.ai.model.AiToolResultContent;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolResult;
import com.mohistmc.youer.util.I18n;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class AiToolExecutor {
    public static final int MAX_RESULT_CHARS = 16_384;

    private final AiToolSchemaValidator validator;
    private final AiToolApproval approval;
    private final AiExecutionDispatcher dispatcher;
    private final BiPredicate<AiToolContext, String> permissionCheck;
    private final Predicate<AiToolContext> onlineCheck;
    private final AiToolAudit audit;

    public AiToolExecutor(
            AiToolSchemaValidator validator,
            AiToolApproval approval,
            AiExecutionDispatcher dispatcher,
            BiPredicate<AiToolContext, String> permissionCheck,
            Predicate<AiToolContext> onlineCheck) {
        this(validator, approval, dispatcher, permissionCheck, onlineCheck,
                new AiToolAudit(java.util.logging.Logger.getLogger("Youer AI Tools")));
    }

    public AiToolExecutor(
            AiToolSchemaValidator validator,
            AiToolApproval approval,
            AiExecutionDispatcher dispatcher,
            BiPredicate<AiToolContext, String> permissionCheck,
            Predicate<AiToolContext> onlineCheck,
            AiToolAudit audit) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.approval = Objects.requireNonNull(approval, "approval");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.permissionCheck = Objects.requireNonNull(permissionCheck, "permissionCheck");
        this.onlineCheck = Objects.requireNonNull(onlineCheck, "onlineCheck");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public CompletionStage<AiToolResultContent> execute(
            AiToolContext context, AiRegisteredTool tool, AiToolCallContent call) {
        long started = System.nanoTime();
        return executeChecked(context, tool, call).whenComplete((result, failure) -> audit.record(
                context.playerId().toString(), tool.definition().name(),
                failure != null ? "failure" : result.error() ? "error" : "success",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
    }

    private CompletionStage<AiToolResultContent> executeChecked(
            AiToolContext context, AiRegisteredTool tool, AiToolCallContent call) {
        String invalid = invalidReason(context, tool, call);
        if (invalid != null) return CompletableFuture.completedFuture(error(call, invalid));
        return approval.request(context, tool, call).thenCompose(decision -> {
            if (decision != AiToolApprovalDecision.APPROVED) {
                return CompletableFuture.completedFuture(error(call, approvalError(decision)));
            }
            String rechecked = invalidReason(context, tool, call);
            if (rechecked != null) return CompletableFuture.completedFuture(error(call, rechecked));
            return dispatcher.dispatch(tool.definition().executionMode(),
                            () -> tool.handler().execute(context, call.arguments()))
                    .toCompletableFuture()
                    .orTimeout(tool.definition().timeout().toMillis(), TimeUnit.MILLISECONDS)
                    .handle((result, failure) -> failure == null
                            ? content(call, result)
                            : error(call, I18n.as(failure instanceof TimeoutException
                                    ? "ai.tool.error.timeout" : "ai.tool.error.execution_failed")));
        });
    }

    private String invalidReason(AiToolContext context, AiRegisteredTool tool, AiToolCallContent call) {
        if (!onlineCheck.test(context)) return I18n.as("ai.tool.error.player_unavailable");
        if (!tool.owner().isEnabled()) return I18n.as("ai.tool.error.unavailable");
        if (!permissionCheck.test(context, tool.definition().permission())) {
            return I18n.as("ai.tool.error.permission_revoked");
        }
        if (!tool.definition().name().equals(call.name())) return I18n.as("ai.tool.error.unavailable");
        List<String> errors = validator.validate(tool.definition().inputSchema(), call.arguments());
        return errors.isEmpty() ? null : I18n.as("ai.tool.error.schema", String.join("; ", errors));
    }

    private static String approvalError(AiToolApprovalDecision decision) {
        return I18n.as(switch (decision) {
            case DENIED -> "ai.tool.error.denied";
            case EXPIRED -> "ai.tool.error.expired";
            case CANCELLED -> "ai.tool.error.cancelled";
            case UNAVAILABLE -> "ai.tool.error.unavailable";
            case APPROVED -> throw new IllegalArgumentException("Approved is not an error");
        });
    }

    private static AiToolResultContent content(AiToolCallContent call, AiToolResult result) {
        return new AiToolResultContent(call.id(), call.name(), truncate(result.content()), result.error());
    }

    private static AiToolResultContent error(AiToolCallContent call, String message) {
        return new AiToolResultContent(call.id(), call.name(), truncate(message), true);
    }

    private static String truncate(String value) {
        return value.length() <= MAX_RESULT_CHARS ? value : value.substring(0, MAX_RESULT_CHARS);
    }
}
