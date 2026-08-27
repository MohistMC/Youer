package com.mohistmc.youer.ai;

import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

public record AiChatInput(Mode mode, String message) {

    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    public static Optional<AiChatInput> parse(String rawMessage, String command) {
        if (rawMessage == null || command == null || command.isBlank()) {
            return Optional.empty();
        }
        String message = normalize(LEGACY_COLOR.matcher(rawMessage).replaceAll(""));
        String name = normalize(command.strip());
        Optional<AiChatInput> broadcast = match(message, "@" + name, Mode.BROADCAST);
        return broadcast.isPresent() ? broadcast : match(message, name, Mode.PRIVATE);
    }

    private static Optional<AiChatInput> match(String message, String command, Mode mode) {
        if (!message.startsWith(command)) {
            return Optional.empty();
        }
        int questionStart = command.length();
        if (questionStart >= message.length()
                || !isWhitespace(message.codePointAt(questionStart))) {
            return Optional.empty();
        }
        while (questionStart < message.length()) {
            int codePoint = message.codePointAt(questionStart);
            if (!isWhitespace(codePoint)) {
                break;
            }
            questionStart += Character.charCount(codePoint);
        }
        String question = message.substring(questionStart).strip();
        return question.isEmpty() ? Optional.empty() : Optional.of(new AiChatInput(mode, question));
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    public enum Mode {
        PRIVATE,
        BROADCAST
    }
}
