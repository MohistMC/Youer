package com.mohistmc.youer.api;

import com.mohistmc.tools.ChineseColors;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * ColorAPI
 * Only use internally, because where it needs to be used externally, we've hardhooked it in the underlying code
 */
public final class ColorAPI {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient(?:[:]#?[0-9A-Fa-f]{6}|[:][\\w\\u4e00-\\u9fa5]+)+>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRADIENT_COLORS = Pattern.compile("[:]([^:>]+)");

    private static final Pattern SOLID_COLOR_PATTERN = Pattern.compile("<(#?[0-9A-Fa-f]{6}|[a-zA-Z_\\u4e00-\\u9fa5]+)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    /**
     * Main color processing method
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String processed = text;
        processed = processGradients(processed);
        processed = processSolidColors(processed);
        processed = processHexColors(processed);
        processed = processLegacyCodes(processed);

        return LEGACY_SERIALIZER.deserialize(processed);
    }

    /**
     * Main color processing method - returns string
     */
    public static String colorizeString(String text) {
        Component component = colorize(text);
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Process all gradient types (both two-color and multi-color)
     */
    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String fullTag = matcher.group(0);
            String content = matcher.group(1);

            List<String> colors = new ArrayList<>();
            Matcher colorMatcher = GRADIENT_COLORS.matcher(fullTag);
            while (colorMatcher.find()) {
                String color = colorMatcher.group(1);
                if (color.startsWith("#")) {
                    color = color.substring(1);
                }
                colors.add(color);
            }

            String gradientText;
            if (colors.size() >= 2) {
                gradientText = createMultiGradient(content, colors);
            } else if (colors.size() == 1) {
                String hex = resolveColorHex(colors.getFirst());
                gradientText = "§x" + convertToHexFormat(hex) + content;
            } else {
                gradientText = content;
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Create multi-color gradient
     */
    private static String createMultiGradient(String text, List<String> colorNames) {
        if (text.isEmpty() || colorNames.size() < 2) return text;

        try {
            List<TextColor> colors = new ArrayList<>();
            for (String colorName : colorNames) {
                String hex = resolveColorHex(colorName);
                TextColor color = TextColor.fromHexString("#" + hex);
                if (color == null) {
                    return "§x" + convertToHexFormat(resolveColorHex(colorNames.getFirst())) + text;
                }
                colors.add(color);
            }

            StringBuilder gradientBuilder = new StringBuilder();
            int length = text.length();
            int segments = colors.size() - 1;

            if (segments <= 0) {
                return text;
            }

            int charsPerSegment = length / segments;
            int remainingChars = length % segments;

            int currentIndex = 0;

            for (int i = 0; i < segments; i++) {
                int segmentLength = charsPerSegment + (i < remainingChars ? 1 : 0);
                if (currentIndex >= length) break;

                int endIndex = Math.min(currentIndex + segmentLength, length);
                String segmentText = text.substring(currentIndex, endIndex);

                TextColor startColor = colors.get(i);
                TextColor endColor = colors.get(i + 1);

                for (int j = 0; j < segmentText.length(); j++) {
                    char c = segmentText.charAt(j);
                    double ratio = segmentText.length() > 1 ? (double) j / (segmentText.length() - 1) : 0.5;

                    TextColor intermediateColor = interpolateColor(startColor, endColor, ratio);
                    String hexColor = String.format("%06X", intermediateColor.value() & 0xFFFFFF);
                    gradientBuilder.append("§x").append(convertToHexFormat(hexColor)).append(c);
                }

                currentIndex = endIndex;
            }

            return gradientBuilder.toString();
        } catch (Exception e) {
            return "§x" + convertToHexFormat(resolveColorHex(colorNames.getFirst())) + text;
        }
    }

    /**
     * Color interpolation calculation
     */
    private static TextColor interpolateColor(TextColor start, TextColor end, double ratio) {
        int startRed = (start.value() >> 16) & 0xFF;
        int startGreen = (start.value() >> 8) & 0xFF;
        int startBlue = start.value() & 0xFF;

        int endRed = (end.value() >> 16) & 0xFF;
        int endGreen = (end.value() >> 8) & 0xFF;
        int endBlue = end.value() & 0xFF;

        int red = (int) (startRed + (endRed - startRed) * ratio);
        int green = (int) (startGreen + (endGreen - startGreen) * ratio);
        int blue = (int) (startBlue + (endBlue - startBlue) * ratio);

        // Ensure color values are within valid range
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return TextColor.color(red, green, blue);
    }

    /**
     * Process solid colors
     */
    private static String processSolidColors(String text) {
        Matcher matcher = SOLID_COLOR_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String color = matcher.group(1);
            String content = matcher.group(2);

            String coloredText;
            if (color.startsWith("#")) {
                String hex = color.substring(1);
                coloredText = "§x" + convertToHexFormat(hex) + content;
            } else if (color.matches("[0-9A-Fa-f]{6}")) {
                coloredText = "§x" + convertToHexFormat(color) + content;
            } else {
                String hex = resolveColorHex(color);
                if (!hex.equals(color)) {
                    coloredText = "§x" + convertToHexFormat(hex) + content;
                } else {
                    String legacyCode = colorNameToLegacy(color);
                    coloredText = legacyCode + content;
                }
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(coloredText));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Process hexadecimal color codes
     */
    private static String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x" + convertToHexFormat(hex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Process legacy color codes
     */
    private static String processLegacyCodes(String text) {
        Matcher matcher = LEGACY_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String code = matcher.group(1);
            // Special handling for reset code
            if (code.equalsIgnoreCase("r")) {
                matcher.appendReplacement(buffer, "§r");
            } else {
                matcher.appendReplacement(buffer, "§" + code);
            }
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Convert hexadecimal format to Minecraft format
     */
    private static String convertToHexFormat(String hex) {
        StringBuilder builder = new StringBuilder();
        for (char c : hex.toCharArray()) {
            builder.append('§').append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    /**
     * Convert color name to legacy color code
     */
    private static String colorNameToLegacy(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> "§0";
            case "dark_blue", "darkblue" -> "§1";
            case "dark_green", "darkgreen" -> "§2";
            case "dark_aqua", "darkaqua" -> "§3";
            case "dark_red", "darkred" -> "§4";
            case "dark_purple", "darkpurple" -> "§5";
            case "gold", "orange" -> "§6";
            case "gray", "grey" -> "§7";
            case "dark_gray", "darkgray", "dark_grey", "darkgrey" -> "§8";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "aqua", "cyan" -> "§b";
            case "red" -> "§c";
            case "light_purple", "lightpurple", "pink", "magenta" -> "§d";
            case "yellow" -> "§e";
            case "white" -> "§f";
            case "bold" -> "§l";
            case "italic" -> "§o";
            case "underlined", "underline" -> "§n";
            case "strikethrough", "strike" -> "§m";
            case "obfuscated", "obfuscate" -> "§k";
            case "reset" -> "§r";
            default -> "";
        };
    }

    /**
     * Resolve color hex code from color name or hex string
     */
    private static String resolveColorHex(String color) {
        if (color.matches("[0-9A-Fa-f]{6}")) {
            return color;
        }

        ChineseColors chineseColor = ChineseColors.getByName(color);
        if (chineseColor != null) {
            return chineseColor.getHexCode().substring(1); // 移除#
        }
        return color;
    }
}
