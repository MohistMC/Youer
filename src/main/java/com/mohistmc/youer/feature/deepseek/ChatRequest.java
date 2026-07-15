package com.mohistmc.youer.feature.deepseek;

import com.mohistmc.mjson.ToJson;
import java.util.List;
import java.util.Objects;

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

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getFrequency_penalty() {
        return frequency_penalty;
    }

    public void setFrequency_penalty(double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }

    public double getPresence_penalty() {
        return presence_penalty;
    }

    public void setPresence_penalty(double presence_penalty) {
        this.presence_penalty = presence_penalty;
    }

    public ResponseFormat getResponse_format() {
        return response_format;
    }

    public void setResponse_format(ResponseFormat response_format) {
        this.response_format = response_format;
    }

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public Object getStream_options() {
        return stream_options;
    }

    public void setStream_options(Object stream_options) {
        this.stream_options = stream_options;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTop_p() {
        return top_p;
    }

    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    public List<Object> getTools() {
        return tools;
    }

    public void setTools(List<Object> tools) {
        this.tools = tools;
    }

    public String getTool_choice() {
        return tool_choice;
    }

    public void setTool_choice(String tool_choice) {
        this.tool_choice = tool_choice;
    }

    public boolean isLogprobs() {
        return logprobs;
    }

    public void setLogprobs(boolean logprobs) {
        this.logprobs = logprobs;
    }

    public Object getTop_logprobs() {
        return top_logprobs;
    }

    public void setTop_logprobs(Object top_logprobs) {
        this.top_logprobs = top_logprobs;
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

    public static class ResponseFormat {
        @ToJson
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "ResponseFormat{" +
                    "type='" + type + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResponseFormat that = (ResponseFormat) o;
            return Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "messages=" + messages +
                ", model='" + model + '\'' +
                ", frequency_penalty=" + frequency_penalty +
                ", max_tokens=" + max_tokens +
                ", presence_penalty=" + presence_penalty +
                ", response_format=" + response_format +
                ", stop='" + stop + '\'' +
                ", stream=" + stream +
                ", stream_options=" + stream_options +
                ", temperature=" + temperature +
                ", top_p=" + top_p +
                ", tools=" + tools +
                ", tool_choice='" + tool_choice + '\'' +
                ", logprobs=" + logprobs +
                ", top_logprobs=" + top_logprobs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRequest that = (ChatRequest) o;
        return Double.compare(that.frequency_penalty, frequency_penalty) == 0 &&
                max_tokens == that.max_tokens &&
                Double.compare(that.presence_penalty, presence_penalty) == 0 &&
                stream == that.stream &&
                Double.compare(that.temperature, temperature) == 0 &&
                Double.compare(that.top_p, top_p) == 0 &&
                logprobs == that.logprobs &&
                Objects.equals(messages, that.messages) &&
                Objects.equals(model, that.model) &&
                Objects.equals(response_format, that.response_format) &&
                Objects.equals(stop, that.stop) &&
                Objects.equals(stream_options, that.stream_options) &&
                Objects.equals(tools, that.tools) &&
                Objects.equals(tool_choice, that.tool_choice) &&
                Objects.equals(top_logprobs, that.top_logprobs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages, model, frequency_penalty, max_tokens, presence_penalty, response_format, stop, stream, stream_options, temperature, top_p, tools, tool_choice, logprobs, top_logprobs);
    }
}
