package com.mohistmc.youer.ai.tool.command;

public final class AiCommandSanitizer {
    public String normalize(String command) {
        if (command == null) throw new IllegalArgumentException("Command is required");
        String value = command.trim();
        if (value.startsWith("/")) value = value.substring(1).trim();
        if (value.isBlank()) throw new IllegalArgumentException("Command must not be blank");
        if (value.indexOf(';') >= 0) throw new IllegalArgumentException("Command chaining is not allowed");
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException("Control characters are not allowed");
            }
        }
        return value;
    }
}
