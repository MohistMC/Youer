package com.mohistmc.youer.ai;

import com.mohistmc.youer.ai.error.AiProviderException;
import com.mohistmc.youer.ai.history.AiConversationStore;
import com.mohistmc.youer.ai.http.UnirestAiHttpClient;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.util.I18n;
import java.util.concurrent.RejectedExecutionException;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AiChatHandler {

    private static final Logger LOGGER = LogManager.getLogger(AiChatHandler.class);
    private static final AiConversationStore HISTORY = new AiConversationStore();
    private static volatile AiChatService service;

    private AiChatHandler() {
    }

    public static synchronized void configure() {
        AiRuntime runtime = AiRuntimeFactory.createFromConfig(new UnirestAiHttpClient());
        if (service == null) {
            service = new AiChatService(runtime, HISTORY);
        } else {
            service.replaceRuntime(runtime);
        }
    }

    public static boolean handle(Player player, String rawMessage) {
        AiChatService current = service;
        if (current == null) {
            configure();
            current = service;
        }
        AiRuntime runtime = current.runtime();
        if (!runtime.enabled() || !hasChatPermission(player)) {
            return false;
        }
        AiChatInput input = AiChatInput.parse(rawMessage, runtime.command(), runtime.allCommand()).orElse(null);
        if (input == null) {
            return false;
        }

        dispatch(() -> {
            if (input.mode() == AiChatInput.Mode.BROADCAST) {
                Bukkit.broadcastMessage("<" + player.getName() + "> " + rawMessage);
            } else {
                player.sendMessage("<" + player.getName() + "> " + rawMessage);
            }
        });
        current.chat(player.getUniqueId(), input.message()).whenComplete((response, failure) ->
                dispatch(() -> complete(player, input, runtime, response, failure)));
        return true;
    }

    public static AiChatService service() {
        return service;
    }

    public static synchronized void close() {
        if (service != null) {
            service.close();
            service = null;
        }
    }

    private static boolean hasChatPermission(Player player) {
        return player.hasPermission("youer.ai.use");
    }

    private static void complete(
            Player player,
            AiChatInput input,
            AiRuntime runtime,
            AiChatResponse response,
            Throwable failure) {
        if (failure == null) {
            String formatted;
            try {
                formatted = runtime.chatFormat().formatted(response.content());
            } catch (RuntimeException exception) {
                formatted = "<AI> " + response.content();
                LOGGER.error("Invalid AI chat format configured; using fallback format");
            }
            if (input.mode() == AiChatInput.Mode.BROADCAST) {
                Bukkit.broadcastMessage(formatted);
            } else {
                player.sendMessage(formatted);
            }
            return;
        }

        Throwable cause = unwrap(failure);
        player.sendMessage(localizedFailure(cause));
        if (cause instanceof AiProviderException providerError) {
            LOGGER.error(
                    "AI request failed: profile={}, provider={}, status={}, requestId={}, error={}",
                    providerError.profile(),
                    providerError.provider(),
                    providerError.status(),
                    providerError.requestId(),
                    providerError.getMessage());
        } else {
            LOGGER.error("AI request failed: {}", cause.getClass().getSimpleName());
        }
    }

    private static String localizedFailure(Throwable cause) {
        if (cause instanceof RejectedExecutionException) {
            return I18n.as("ai.busy");
        }
        if (cause instanceof IllegalStateException) {
            return I18n.as("ai.unavailable");
        }
        if (cause instanceof AiProviderException error) {
            return switch (error.type()) {
                case AUTHENTICATION -> I18n.as("ai.error.authentication");
                case RATE_LIMIT -> I18n.as("ai.error.rate_limit");
                case TIMEOUT -> I18n.as("ai.error.timeout");
                case INVALID_RESPONSE, EMPTY_RESPONSE -> I18n.as("ai.error.invalid_response");
                default -> I18n.as("ai.error.request_failed");
            };
        }
        return I18n.as("ai.error.request_failed");
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static void dispatch(Runnable action) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            server.execute(action);
        } else {
            action.run();
        }
    }
}
