package com.mohistmc.youer.ai;

import java.util.Optional;
import java.util.regex.Pattern;

public record AiChatInput(Mode mode, String message) {

    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    public static Optional<AiChatInput> parse(String rawMessage, String privateCommand, String broadcastCommand) {
        if (rawMessage == null) {
            return Optional.empty();
        }
        String message = LEGACY_COLOR.matcher(rawMessage).replaceAll("");
        Optional<AiChatInput> broadcast = match(message, broadcastCommand, Mode.BROADCAST);
        return broadcast.isPresent() ? broadcast : match(message, privateCommand, Mode.PRIVATE);
    }

    private static Optional<AiChatInput> match(String message, String command, Mode mode) {
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }
        String prefix = command.trim() + " ";
        if (!message.startsWith(prefix)) {
            return Optional.empty();
        }
        String question = message.substring(prefix.length()).trim();
        return question.isEmpty() ? Optional.empty() : Optional.of(new AiChatInput(mode, question));
    }

    public enum Mode {
        PRIVATE,
        BROADCAST
    }
}
