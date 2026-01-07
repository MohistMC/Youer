package com.mohistmc.youer.ai.deepseek;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DeepSeek {

    // Store conversation history for each player
    private static final Map<UUID, List<ChatRequest.Message>> conversationHistory = new HashMap<>();

    public static void init(Player player, String msg) {
        if (YouerConfig.deepseek_enable && player.hasPermission("youer.ai.deepseek")) {
            String plainMsg = ChatColor.stripColor(msg);
            
            String trigger = YouerConfig.deepseek_command + " ";
            if (plainMsg.startsWith(trigger)) {
                String message = plainMsg.substring(trigger.length());
                CompletableFuture.supplyAsync(() -> chatWithMemory(player, message))
                        .thenAccept(reply -> {
                            if (reply != null) {
                                player.sendMessage(MiniMessage.miniMessage().deserialize(YouerConfig.deepseek_chatformat.formatted(reply)));
                                updateHistory(player.getUniqueId(), message, reply);
                            }
                        }).exceptionally(ex -> {
                            Youer.LOGGER.error("DeepSeek chat failed", ex);
                            player.sendMessage(MiniMessage.miniMessage().deserialize(I18n.as("deepseek.error.exception", ex.getMessage())));
                            return null;
                        });
                return;
            }

            String all_trigger = YouerConfig.deepseek_all_command + " ";
            if (plainMsg.startsWith(all_trigger)) {
                String message = plainMsg.substring(all_trigger.length());
                CompletableFuture.supplyAsync(() -> chatWithMemory(player, message))
                        .thenAccept(reply -> {
                            if (reply != null) {
                                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(YouerConfig.deepseek_chatformat.formatted(reply)));
                                updateHistory(player.getUniqueId(), message, reply);
                            }
                        }).exceptionally(ex -> {
                            Youer.LOGGER.error("DeepSeek chat-all failed", ex);
                            player.sendMessage(MiniMessage.miniMessage().deserialize(I18n.as("deepseek.error.exception", ex.getMessage())));
                            return null;
                        });
            }
        }
    }

    /**
     * Chat method with memory
     */
    public static String chatWithMemory(Player player, String msg) {
        ChatRequest request = new ChatRequest();
        request.setModel(YouerConfig.deepseek_model);
        request.setFrequency_penalty(0);
        request.setMax_tokens(2048);
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
            throw new RuntimeException(I18n.as("deepseek.error.request_failed", response.getStatus(), response.getBody()));
        }

        Json json = Json.read(response.getBody());
        if (json == null || json.isNull()) {
             throw new RuntimeException(I18n.as("deepseek.error.empty_response"));
        }

        ChatCompletion chatCompletion = json.asBean(ChatCompletion.class);
        if (chatCompletion.getChoices() == null || chatCompletion.getChoices().length == 0) {
             throw new RuntimeException(I18n.as("deepseek.error.no_choices", response.getBody()));
        }
        return chatCompletion.getChoices()[0].getMessage().getContent();
    }

    /**
     * Update conversation history
     */
    private static void updateHistory(UUID playerId, String userMessage, String aiResponse) {
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
}
