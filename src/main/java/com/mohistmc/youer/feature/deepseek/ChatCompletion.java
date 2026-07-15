package com.mohistmc.youer.feature.deepseek;

import com.mohistmc.mjson.ToJson;
import java.util.Arrays;
import java.util.Objects;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Choice[] getChoices() {
        return choices;
    }

    public void setChoices(Choice[] choices) {
        this.choices = choices;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public static class Choice {

        @ToJson
        private String finish_reason;

        @ToJson
        private int index;

        @ToJson
        private Message message;

        public String getFinish_reason() {
            return finish_reason;
        }

        public void setFinish_reason(String finish_reason) {
            this.finish_reason = finish_reason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Choice{" +
                    "finish_reason='" + finish_reason + '\'' +
                    ", index=" + index +
                    ", message=" + message +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Choice choice = (Choice) o;
            return index == choice.index &&
                    Objects.equals(finish_reason, choice.finish_reason) &&
                    Objects.equals(message, choice.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(finish_reason, index, message);
        }

        public static class Message {

            @ToJson
            private String content;

            @ToJson
            private String role;

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            @Override
            public String toString() {
                return "Message{" +
                        "content='" + content + '\'' +
                        ", role='" + role + '\'' +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Message message = (Message) o;
                return Objects.equals(content, message.content) &&
                        Objects.equals(role, message.role);
            }

            @Override
            public int hashCode() {
                return Objects.hash(content, role);
            }
        }
    }

    public static class Usage {

        @ToJson
        private int completion_tokens;

        @ToJson
        private int prompt_tokens;

        @ToJson
        private int total_tokens;

        public int getCompletion_tokens() {
            return completion_tokens;
        }

        public void setCompletion_tokens(int completion_tokens) {
            this.completion_tokens = completion_tokens;
        }

        public int getPrompt_tokens() {
            return prompt_tokens;
        }

        public void setPrompt_tokens(int prompt_tokens) {
            this.prompt_tokens = prompt_tokens;
        }

        public int getTotal_tokens() {
            return total_tokens;
        }

        public void setTotal_tokens(int total_tokens) {
            this.total_tokens = total_tokens;
        }

        @Override
        public String toString() {
            return "Usage{" +
                    "completion_tokens=" + completion_tokens +
                    ", prompt_tokens=" + prompt_tokens +
                    ", total_tokens=" + total_tokens +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Usage usage = (Usage) o;
            return completion_tokens == usage.completion_tokens &&
                    prompt_tokens == usage.prompt_tokens &&
                    total_tokens == usage.total_tokens;
        }

        @Override
        public int hashCode() {
            return Objects.hash(completion_tokens, prompt_tokens, total_tokens);
        }
    }

    @Override
    public String toString() {
        return "ChatCompletion{" +
                "id='" + id + '\'' +
                ", choices=" + Arrays.toString(choices) +
                ", created=" + created +
                ", model='" + model + '\'' +
                ", object='" + object + '\'' +
                ", usage=" + usage +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatCompletion that = (ChatCompletion) o;
        return created == that.created &&
                Objects.equals(id, that.id) &&
                Arrays.equals(choices, that.choices) &&
                Objects.equals(model, that.model) &&
                Objects.equals(object, that.object) &&
                Objects.equals(usage, that.usage);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, created, model, object, usage);
        result = 31 * result + Arrays.hashCode(choices);
        return result;
    }
}
