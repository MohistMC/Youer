package com.mohistmc.youer.ai.deepseek;

import com.mohistmc.mjson.ToJson;
import lombok.Data;

@Data
public class ChatCompletion {

    @ToJson
    private String id;

    @ToJson
    private Choice[] choices;

    @ToJson
    private long created;

    @ToJson
    private String model;

    @ToJson
    private String object;

    @ToJson
    private Usage usage;

    @Data
    public static class Choice {

        @ToJson
        private String finish_reason;

        @ToJson
        private int index;

        @ToJson
        private Message message;

        @Data
        public static class Message {

            @ToJson
            private String content;

            @ToJson
            private String role;
        }
    }

    @Data
    public static class Usage {

        @ToJson
        private int completion_tokens;

        @ToJson
        private int prompt_tokens;

        @ToJson
        private int total_tokens;
    }
}