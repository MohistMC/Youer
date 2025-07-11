package com.mohistmc.youer.ai.deepseek;

import com.mohistmc.mjson.ToJson;
import java.util.List;
import lombok.Data;

@Data
public class ChatRequest {

    @ToJson
    private List<Message> messages;
    @ToJson
    private String model;
    @ToJson
    private double frequency_penalty;
    @ToJson
    private int max_tokens;
    @ToJson
    private double presence_penalty;
    @ToJson
    private ResponseFormat response_format;
    @ToJson
    private String stop;
    @ToJson
    private boolean stream;
    @ToJson
    private Object stream_options;
    @ToJson
    private double temperature;
    @ToJson
    private double top_p;
    @ToJson
    private List<Object> tools;
    @ToJson
    private String tool_choice;
    @ToJson
    private boolean logprobs;
    @ToJson
    private Object top_logprobs;

    @Data
    public static class Message {
        @ToJson
        private String content;
        @ToJson
        private String role;
    }

    @Data
    public static class ResponseFormat {
        @ToJson
        private String type;
    }
}
