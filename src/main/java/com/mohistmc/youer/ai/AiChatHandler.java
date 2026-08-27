package com.mohistmc.youer.ai;

import com.mohistmc.youer.ai.error.AiProviderException;
import com.mohistmc.youer.ai.history.AiConversationStore;
import com.mohistmc.youer.ai.http.UnirestAiHttpClient;
import com.mohistmc.youer.ai.model.AiChatResponse;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolDefinition;
import com.mohistmc.youer.api.ai.tool.AiToolExecutionMode;
import com.mohistmc.youer.api.ai.tool.AiToolRisk;
import com.mohistmc.youer.util.I18n;
import java.util.concurrent.RejectedExecutionException;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.mohistmc.mjson.Json;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.ai.tool.*;
import com.mohistmc.youer.ai.tool.command.*;
import com.mohistmc.youer.ai.tool.confirmation.*;
import com.mohistmc.youer.ai.tool.http.*;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.nio.file.Path;
import java.util.function.Function;
import com.mohistmc.youer.ai.skill.*;

public final class AiChatHandler {

    private static final Logger LOGGER = LogManager.getLogger(AiChatHandler.class);
    private static final AiConversationStore HISTORY = new AiConversationStore();
    private static volatile AiChatService service;
    private static final AiToolSchemaValidator TOOL_SCHEMA = new AiToolSchemaValidator();
    private static final AiToolRegistry TOOL_REGISTRY = new AiToolRegistry(TOOL_SCHEMA);
    private static final AiConfirmationStore CONFIRMATIONS = new AiConfirmationStore(Clock.systemUTC());

    private AiChatHandler() {
    }

    private static synchronized void initialize() {
        if (service != null) {
            return;
        }
        AiRuntime runtime = AiRuntimeFactory.createFromConfig(new UnirestAiHttpClient());
        AiToolPermissions.registerDefaults();
        AiSkillCatalog skillCatalog = new AiSkillLoader(new AiSkillParser(), LOGGER).load(
                AiChatHandler.class.getClassLoader(), "ai/skills/index.txt",
                Path.of("youer-config", "ai", "skills"));
        AiSkillRegistry skillRegistry = new AiSkillRegistry(skillCatalog);
        skillRegistry.registerPermissions();
        Function<AiToolContext, AiSkillAccess> skillAccess = context ->
                new BukkitAiSkillAccess(Bukkit.getPlayer(context.playerId()));
        AiToolOwner owner = new AiToolOwner("runtime", AiToolSource.BUILT_IN, () -> true);
        List<AiRegisteredTool> catalog = builtIns(owner, skillRegistry, skillAccess);
        TOOL_REGISTRY.initializeRuntimeTools(owner, catalog);
        AiToolRegistry.activate(TOOL_REGISTRY);
        AiConfirmationNotifier notifier = new AiConfirmationNotifier(Bukkit::getPlayer);
        AiConfirmationApproval approval = new AiConfirmationApproval(CONFIRMATIONS,
                Duration.ofSeconds(runtime.confirmationTimeoutSeconds()),
                runtime.playerCommandsRequireConfirmation(), notifier::notify);
        AiExecutionDispatcher dispatcher = new AiExecutionDispatcher(ForkJoinPool.commonPool(), AiChatHandler::dispatch);
        AiToolExecutor toolExecutor = new AiToolExecutor(TOOL_SCHEMA, approval, dispatcher,
                (context, permission) -> {
                    Player player = Bukkit.getPlayer(context.playerId());
                    return player != null && player.hasPermission("youer.ai.use")
                            && player.hasPermission("youer.ai.tools.use") && player.hasPermission(permission);
                }, context -> Bukkit.getPlayer(context.playerId()) != null);
        AiCapabilitySnapshotProvider capabilitySnapshots = new AiCapabilitySnapshotProvider(
                dispatcher, TOOL_REGISTRY, skillRegistry, skillAccess, new AiSkillIndex());
        service = new AiChatService(
                runtime, HISTORY, new AiAgentLoop(toolExecutor), capabilitySnapshots);
    }

    public static boolean handle(Player player, String rawMessage) {
        AiChatService current = service;
        if (current == null) {
            initialize();
            current = service;
        }
        AiRuntime runtime = current.runtime();
        if (!runtime.enabled() || !hasChatPermission(player)) {
            return false;
        }
        AiChatInput input = AiChatInput.parse(rawMessage, runtime.command()).orElse(null);
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
        current.chat(new AiToolContext(player.getUniqueId(), player.getName(), player.locale()), input.message())
                .whenComplete((response, failure) ->
                dispatch(() -> complete(player, input, runtime, response, failure)));
        return true;
    }

    public static AiChatService service() {
        return service;
    }

    public static boolean confirm(UUID playerId, String id) { return CONFIRMATIONS.confirm(playerId, id); }
    public static boolean cancel(UUID playerId, String id) { return CONFIRMATIONS.cancel(playerId, id); }
    public static AiToolRegistry.Snapshot tools(Player player) {
        return TOOL_REGISTRY.snapshot(permission -> player.hasPermission("youer.ai.use")
                && player.hasPermission("youer.ai.tools.use") && player.hasPermission(permission));
    }

    private static List<AiRegisteredTool> builtIns(
            AiToolOwner owner,
            AiSkillRegistry skillRegistry,
            Function<AiToolContext, AiSkillAccess> skillAccess) {
        BukkitAiCommandGateway gateway = new BukkitAiCommandGateway();
        AiCommandSanitizer sanitizer = new AiCommandSanitizer();
        Json commandSchema = Json.object().set("type", "object")
                .set("properties", Json.object().set("command", Json.object().set("type", "string").set("minLength", 1)))
                .set("required", Json.array().add("command")).set("additionalProperties", false);
        Json searchSchema = Json.object().set("type", "object")
                .set("properties", Json.object()
                        .set("query", Json.object().set("type", "string"))
                        .set("mode", Json.object().set("type", "string")
                                .set("enum", Json.array().add("player").add("console"))))
                .set("additionalProperties", false);
        List<AiRegisteredTool> tools = new ArrayList<>();
        tools.add(TOOL_REGISTRY.registered(owner, LoadSkillTool.definition(),
                new LoadSkillTool(skillRegistry, TOOL_REGISTRY, skillAccess)));
        tools.add(TOOL_REGISTRY.registered(owner, new AiToolDefinition("search_commands", "Search visible Minecraft commands",
                        searchSchema, "youer.ai.tools.use", AiToolRisk.READ_ONLY,
                        AiToolExecutionMode.MAIN_THREAD, Duration.ofSeconds(5)),
                        new SearchCommandsTool(gateway, Bukkit::getPlayer)));
        tools.add(TOOL_REGISTRY.registered(owner, new AiToolDefinition("execute_player_command", "Execute one command as the player",
                        commandSchema, "youer.ai.tools.command.player", AiToolRisk.PLAYER_ACTION,
                        AiToolExecutionMode.MAIN_THREAD, Duration.ofSeconds(10)),
                        new PlayerCommandTool(gateway, sanitizer, Bukkit::getPlayer)));
        tools.add(TOOL_REGISTRY.registered(owner, new AiToolDefinition("execute_console_command", "Execute one command as the server console",
                        commandSchema, "youer.ai.tools.command.console", AiToolRisk.SERVER_ACTION,
                        AiToolExecutionMode.MAIN_THREAD, Duration.ofSeconds(10)),
                        new ConsoleCommandTool(gateway, sanitizer)));
        AiToolOwner httpOwner = new AiToolOwner(owner.id(), AiToolSource.CONFIGURED_HTTP, () -> true);
        AiExternalHttpTransport transport = new JdkAiExternalHttpTransport();
        for (AiHttpToolDefinition definition : new AiHttpToolParser().parse(YouerConfig.ai_http_tools)) {
            AiToolPermissions.ensureOpDefault(definition.tool().permission());
            tools.add(TOOL_REGISTRY.registered(httpOwner, definition.tool(),
                    new AiHttpToolHandler(definition, transport)));
        }
        return List.copyOf(tools);
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
                    "AI request failed: provider={}, status={}, requestId={}, error={}",
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
