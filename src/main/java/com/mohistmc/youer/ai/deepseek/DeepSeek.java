package com.mohistmc.youer.ai.deepseek;

import com.mohistmc.mjson.Json;
import com.mohistmc.youer.YouerConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DeepSeek {

    public static void init(Player player, String msg) {
        if (YouerConfig.deepseek_enable && player.hasPermission("youer.ai.deepseek")) {
            String cmd = YouerConfig.deepseek_command + " ";
            if (msg.startsWith(cmd)) {
                String message = msg.replace(cmd, "");
                CompletableFuture.supplyAsync(() -> chat(message))
                        .thenAccept(reply -> player.sendMessage(MiniMessage.miniMessage().deserialize(YouerConfig.deepseek_chatformat.formatted(reply))));
            }

            String all_cmd = YouerConfig.deepseek_all_command + " ";
            if (msg.startsWith(all_cmd)) {
                String message = msg.replace(all_cmd, "");
                CompletableFuture.supplyAsync(() -> chat(message))
                        .thenAccept(reply -> Bukkit.broadcast(MiniMessage.miniMessage().deserialize(YouerConfig.deepseek_chatformat.formatted(reply))));
            }
        }
    }

    public static String chat(String msg) {
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

        ChatRequest.Message systemMessage = new ChatRequest.Message();
        systemMessage.setRole("system");
        systemMessage.setContent(YouerConfig.deepseek_system);

        ChatRequest.Message userMessage = new ChatRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(msg);

        request.setMessages(List.of(systemMessage, userMessage));
        HttpResponse<String> response = Unirest.post(YouerConfig.deepseek_baseUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer %s".formatted(YouerConfig.deepseek_apikey))
                .body(Json.readBean(request).toString())
                .asString();
        Json json = Json.read(response.getBody());
        ChatCompletion chatCompletion = json.asBean(ChatCompletion.class);
        return chatCompletion.getChoices()[0].getMessage().getContent();
    }
}
