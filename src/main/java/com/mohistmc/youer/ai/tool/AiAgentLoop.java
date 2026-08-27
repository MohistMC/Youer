package com.mohistmc.youer.ai.tool;

import com.mohistmc.youer.ai.history.AiConversationTurn;
import com.mohistmc.youer.ai.model.AiChatRequest;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import com.mohistmc.youer.ai.model.AiToolCallContent;
import com.mohistmc.youer.ai.provider.AiToolCapabilityException;
import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class AiAgentLoop {
    private final AiToolExecutor executor;

    public AiAgentLoop(AiToolExecutor executor) {
        this.executor = executor;
    }

    public CompletionStage<AiAgentResult> run(AiAgentRequest request, Executor providerExecutor) {
        ArrayList<AiMessage> messages = new ArrayList<>(request.messages());
        int turnStart = lastUserIndex(messages);
        return step(request, messages, turnStart, 0, 0, false, providerExecutor);
    }

    private CompletionStage<AiAgentResult> step(
            AiAgentRequest request, ArrayList<AiMessage> messages, int turnStart,
            int steps, int calls, boolean plainFallback, Executor providerExecutor) {
        if (steps >= request.maxSteps()) return failed(I18n.as("ai.tool.error.step_limit"));
        List<AiToolDefinition> definitions =
                !plainFallback && request.provider().capabilities().toolCalling()
                        ? request.tools().definitions() : List.of();
        return invokeProvider(request, messages, definitions, providerExecutor)
                .handle((response, failure) -> {
                    if (failure == null) {
                        return continueStep(request, messages, turnStart, steps, calls,
                                plainFallback, providerExecutor, response);
                    }
                    Throwable cause = unwrap(failure);
                    if (cause instanceof AiToolCapabilityException
                            && !plainFallback && !definitions.isEmpty()) {
                        return step(request, messages, turnStart, steps, calls, true, providerExecutor);
                    }
                    return CompletableFuture.<AiAgentResult>failedFuture(cause);
                })
                .thenCompose(Function.identity());
    }

    private CompletionStage<AiAgentResult> continueStep(
            AiAgentRequest request, ArrayList<AiMessage> messages, int turnStart,
            int steps, int calls, boolean plainFallback, Executor providerExecutor,
            AiChatResponse response) {
        List<AiToolCallContent> toolCalls = response.message().content().stream()
                .filter(AiToolCallContent.class::isInstance).map(AiToolCallContent.class::cast).toList();
        messages.add(response.message());
        if (toolCalls.isEmpty()) {
            return CompletableFuture.completedFuture(new AiAgentResult(response,
                    new AiConversationTurn(messages.subList(turnStart, messages.size()))));
        }
        if (calls + toolCalls.size() > request.maxCallsPerTurn()) {
            return failed(I18n.as("ai.tool.error.call_limit"));
        }
        List<AiRegisteredTool> resolved = new ArrayList<>();
        for (AiToolCallContent call : toolCalls) {
            AiRegisteredTool tool = request.tools().find(call.name());
            if (tool == null) return failed(I18n.as("ai.tool.error.unavailable"));
            resolved.add(tool);
        }
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (int index = 0; index < toolCalls.size(); index++) {
            AiToolCallContent call = toolCalls.get(index);
            AiRegisteredTool tool = resolved.get(index);
            chain = chain.thenCompose(ignored -> executor.execute(request.context(), tool, call)
                    .thenAccept(result -> messages.add(new AiMessage(
                            AiRole.TOOL, List.of(result), Map.of()))));
        }
        int nextCalls = calls + toolCalls.size();
        return chain.thenCompose(ignored -> step(
                request, messages, turnStart, steps + 1, nextCalls, plainFallback, providerExecutor));
    }

    private static CompletionStage<AiChatResponse> invokeProvider(
            AiAgentRequest request, List<AiMessage> messages,
            List<AiToolDefinition> definitions, Executor providerExecutor) {
        return CompletableFuture.supplyAsync(
                        () -> request.provider().chat(new AiChatRequest(messages, definitions)), providerExecutor)
                .thenCompose(Function.identity());
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static int lastUserIndex(List<AiMessage> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index).role() == AiRole.USER) return index;
        }
        throw new IllegalArgumentException("Agent request requires a user message");
    }

    private static <T> CompletionStage<T> failed(String message) {
        return CompletableFuture.failedFuture(new IllegalStateException(message));
    }
}
