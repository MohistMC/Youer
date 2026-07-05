package com.mohistmc.youer.ai.deepseek;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.ColorAPI;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DeepSeek {

    public static Logger LOGGER = LogManager.getLogger(DeepSeek.class);
    // Store conversation history for each player
    @Getter
    private static final Map<UUID, List<ChatRequest.Message>> conversationHistory = new ConcurrentHashMap<>();

    public static boolean init(Player player, String msg) {
        if (YouerConfig.deepseek_enable && player.hasPermission("youer.ai.deepseek")) {
            String cmd = YouerConfig.deepseek_command + " ";
            String all_cmd = YouerConfig.deepseek_all_command + " ";
            String strippedMsg = ColorAPI.stripColors(msg);
            if (strippedMsg.startsWith(cmd)) {
                String chatMsg = strippedMsg.substring(cmd.length());
                player.sendMessage("<" + player.getName() + "> " + msg);
                handleCommand(player, chatMsg, false);
                return true;
            } else if (strippedMsg.startsWith(all_cmd)) {
                String chatMsg = strippedMsg.substring(all_cmd.length());
                Bukkit.broadcastMessage("<" + player.getName() + "> " + msg);
                handleCommand(player, chatMsg, true);
                return true;
            }
        }
        return false;
    }

    private static void handleCommand(Player player, String message, boolean isBroadcast) {
        CompletableFuture.supplyAsync(() -> chatWithMemory(player, message))
                .thenAccept(reply -> {
                    if (reply != null) {
                        if (isBroadcast) {
                            Bukkit.broadcastMessage(YouerConfig.deepseek_chatformat.formatted(reply));
                        } else {
                            player.sendMessage(YouerConfig.deepseek_chatformat.formatted(reply));
                        }
                        // Update history
                        updateHistory(player.getUniqueId(), message, reply);
                    }
                })
                .exceptionally(throwable -> {
                    player.sendMessage(YouerConfig.deepseek_chatformat.formatted(Youer.i18n.as("deepseek.error.throwable")));
                    Throwable cause = throwable;
                    while (cause.getCause() != null && cause != cause.getCause()) {
                        cause = cause.getCause();
                    }
                    String errorMsg = cause.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = cause.toString();
                    }
                    LOGGER.error(errorMsg);
                    return null;
                });
    }

    /**
     * Verify that the model name is legitimate
     * <a href="https://api-docs.deepseek.com/zh-cn/api/list-models">...</a>
     */
    private static boolean isValidModel(String model) {
        return "deepseek-v4-flash".equals(model) || "deepseek-v4-pro".equals(model);
    }

    /**
     * Chat method with memory
     */
    public static String chatWithMemory(Player player, String msg) {
        if (!isValidModel(YouerConfig.deepseek_model)) {
            throw new RuntimeException(I18n.as("deepseek.error.invalid_model", YouerConfig.deepseek_model));
        }
        ChatRequest request = new ChatRequest();
        request.setModel(YouerConfig.deepseek_model);
        request.setFrequency_penalty(0);
        request.setMax_tokens(YouerConfig.deepseek_max_tokens);
        request.setPresence_penalty(0);
        ChatRequest.ResponseFormat responseFormat = new ChatRequest.ResponseFormat();
        responseFormat.setType("text");
        request.setResponse_format(responseFormat);
        request.setStop(null);
        request.setStream(false);
        request.setStream_options(null);
        request.setTemperature(1);
        request.setTop_p(1);
        request.setTools(null);
        request.setTool_choice("none");
        request.setLogprobs(false);
        request.setTop_logprobs(null);

        // Build message history
        List<ChatRequest.Message> messages = new ArrayList<>();

        // Add system message
        ChatRequest.Message systemMessage = new ChatRequest.Message();
        systemMessage.setRole("system");
        systemMessage.setContent(YouerConfig.deepseek_system);
        messages.add(systemMessage);

        // Get player's conversation history
        UUID playerId = player.getUniqueId();
        List<ChatRequest.Message> history = conversationHistory.getOrDefault(playerId, new ArrayList<>());

        // Enforce max history size: remove oldest pair if at limit
        int maxHistory = Math.max(YouerConfig.deepseek_max_history, 1);
        while (history.size() >= maxHistory) {
            history.removeFirst();
        }

        // Add history messages
        messages.addAll(history);

        // Add current user message
        ChatRequest.Message userMessage = new ChatRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(msg);
        messages.add(userMessage);

        request.setMessages(messages);

        HttpResponse<String> response = Unirest.post(YouerConfig.deepseek_baseUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer %s".formatted(YouerConfig.deepseek_apikey))
                .body(Json.readBean(request).toString())
                .asString();

        if (!response.isSuccess()) {
            String errorBody = response.getBody();
            if (errorBody == null || errorBody.trim().isEmpty()) {
                errorBody = "No error message returned from API";
            } else {
                try {
                    Json errorJson = Json.read(errorBody);
                    if (errorJson != null && errorJson.has("error") && errorJson.at("error").has("message")) {
                        errorBody = errorJson.at("error").at("message").asString();
                    }
                } catch (Exception ignored) {
                }
            }
            throw new RuntimeException(I18n.as("deepseek.error.request_failed", response.getStatus(), errorBody));
        }
        Json json = Json.read(response.getBody());
        if (json == null || json.isNull()) {
            throw new RuntimeException(I18n.as("deepseek.error.empty_response"));
        }
        ChatCompletion chatCompletion = json.asBean(ChatCompletion.class);
        if (chatCompletion.getChoices() == null || chatCompletion.getChoices().length == 0) {
            throw new RuntimeException(I18n.as("deepseek.error.no_choices", response.getBody()));
        }
        ChatCompletion.Choice choice = chatCompletion.getChoices()[0];
        if (choice.getMessage() == null || choice.getMessage().getContent() == null) {
            throw new RuntimeException(I18n.as("deepseek.error.no_content", response.getBody()));
        }
        return choice.getMessage().getContent();
    }

    /**
     * Update conversation history
     */
    private static void updateHistory(UUID playerId, String userMessage, String aiResponse) {
        if (playerId == null || userMessage == null || aiResponse == null) {
            return;
        }
        List<ChatRequest.Message> history = conversationHistory.computeIfAbsent(playerId, k -> new ArrayList<>());

        // Add user message
        ChatRequest.Message userMsg = new ChatRequest.Message();
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        history.add(userMsg);

        // Add AI response
        ChatRequest.Message assistantMsg = new ChatRequest.Message();
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        history.add(assistantMsg);
    }

    /**
     * Clear conversation history for specific player
     */
    public static void clearHistory(UUID playerId) {
        conversationHistory.remove(playerId);
    }

    /**
     * Clear conversation history for all players
     */
    public static void clearAllHistory() {
        conversationHistory.clear();
    }

    /**
     * Get the number of conversation history for a specific player
     */
    public static int getHistorySize(UUID playerId) {
        List<ChatRequest.Message> history = conversationHistory.get(playerId);
        return history != null ? history.size() : 0;
    }

    /**
     * Get the number of conversation history for all players
     */
    public static int getAllHistorySize() {
        return conversationHistory.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
