package com.mohistmc.youer.api;

import com.mohistmc.tools.ChineseColors;
import com.mohistmc.youer.feature.GlobalVariableSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * ColorAPI
 * Only use internally, because where it needs to be used externally, we've hardhooked it in the underlying code
 */
public class ColorAPI {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient(?:[:]#?[0-9A-Fa-f]{6}|[:][\\w\\u4e00-\\u9fa5]+)+>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRADIENT_COLORS = Pattern.compile("[:]([^:>]+)");

    private static final Pattern SOLID_COLOR_PATTERN = Pattern.compile("<(#?[0-9A-Fa-f]{6}|[a-zA-Z_\\u4e00-\\u9fa5]+)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    // Matches #word, #中文, #hex broadly — resolution trims to longest valid name
    private static final Pattern SHORT_COLOR_PATTERN = Pattern.compile("#([\\w\\u4e00-\\u9fa5]+)");

    // Precompiled patterns for stripColors — single-pass optimized
    private static final Pattern STRIP_GRADIENT = Pattern.compile("<gradient(?:[:]#?[0-9A-Fa-f]{6}|[:][\\w\\u4e00-\\u9fa5]+)+>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIP_SOLID = Pattern.compile("<(#?[0-9A-Fa-f]{6}|[a-zA-Z_\\u4e00-\\u9fa5]+)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIP_HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern STRIP_LEGACY = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern STRIP_SECTION = Pattern.compile("§[0-9a-fk-orA-FK-ORx]");

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .build();

    private static String processText(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }

        String processed = text;
        processed = processGradients(processed);
        processed = processSolidColors(processed);
        processed = processHexColors(processed);   // 处理 &#ff0000
        processed = processLegacyCodes(processed); // 处理 &c
        processed = processShortColorCodes(processed); // 处理 #ff0000 或 #碧色
        return GlobalVariableSystem.as(null, processed);
    }

    /**
     * Main color processing method - returns net.kyori.adventure.text.Component
     */
    public static Component adventure(String text) {
        return LEGACY_SERIALIZER.deserialize(processText(text));
    }

    /**
     * Main color processing method - returns net.kyori.adventure.text.Component
     */
    public static Component adventureOrNull(String text) {
        return LEGACY_SERIALIZER.deserializeOrNull(processText(text));
    }

    /**
     * Main color processing method - returns string
     */
    public static String string(String text) {
        return processText(text);
    }

    /**
     * Main color processing method - returns net.minecraft.network.chat.Component
     */
    public static net.minecraft.network.chat.Component vanilla(String text) {
        return net.minecraft.network.chat.Component.literal(string(text));
    }

    /**
     * Process color codes and return ANSI-formatted string with true color (24-bit RGB) support.
     * Unlike ANSIComponentSerializer which may quantize colors, this always outputs true-color sequences.
     */
    public static String ansi(String text) {
        return ansi(adventure(text));
    }

    /**
     * Convert an Adventure Component to an ANSI-formatted string with true color (24-bit RGB) support.
     */
    public static String ansi(Component component) {
        final StringBuilder sb = new StringBuilder();
        appendAnsi(sb, component);
        return sb.toString();
    }

    private static final char ANSI_ESC = 0x1B;

    private static void appendAnsi(StringBuilder sb, Component component) {
        final TextColor color = component.style().color();
        if (color != null) {
            final int r = (color.value() >> 16) & 0xFF;
            final int g = (color.value() >> 8) & 0xFF;
            final int b = color.value() & 0xFF;
            sb.append(ANSI_ESC).append("[38;2;").append(r).append(';').append(g).append(';').append(b).append('m');
        }
        if (component.style().decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE) {
            sb.append(ANSI_ESC).append("[1m");
        }
        if (component.style().decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE) {
            sb.append(ANSI_ESC).append("[3m");
        }
        if (component.style().decoration(TextDecoration.UNDERLINED) == TextDecoration.State.TRUE) {
            sb.append(ANSI_ESC).append("[4m");
        }
        if (component.style().decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE) {
            sb.append(ANSI_ESC).append("[9m");
        }
        if (component.style().decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE) {
            sb.append(ANSI_ESC).append("[8m");
        }
        if (component instanceof TextComponent text) {
            sb.append(text.content());
        }
        for (final Component child : component.children()) {
            appendAnsi(sb, child);
        }
        if (color != null) {
            sb.append(ANSI_ESC).append("[0m");
        }
    }

    /**
     * Process all gradient types (both two-color and multicolor)
     */
    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();

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
        red = Math.clamp(red, 0, 255);
        green = Math.clamp(green, 0, 255);
        blue = Math.clamp(blue, 0, 255);

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
            return chineseColor.getHexCode().substring(1);
        }
        return color;
    }

    /**
     * Remove all color codes and return to plain text
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }

        String result = text;
        result = STRIP_GRADIENT.matcher(result).replaceAll("$2");
        result = STRIP_SOLID.matcher(result).replaceAll("$2");
        result = STRIP_HEX.matcher(result).replaceAll("");
        result = STRIP_LEGACY.matcher(result).replaceAll("");
        result = STRIP_SECTION.matcher(result).replaceAll("");
        // Remove §x§#§#§#§#§#§# format (hex color chain)
        result = result.replaceAll("§x§[0-9a-fA-F]{6}", "");
        return result;
    }

    /**
     * Try to resolve {@code candidate} as a color name via {@link #resolveColorHex}
     * or {@link #colorNameToLegacy}. Returns the legacy/hex replacement string, or
     * null if the name is not a recognised color.
     */
    private static String tryResolveColor(String candidate) {
        if (candidate.matches("[0-9A-Fa-f]{6}")) {
            return "§x" + convertToHexFormat(candidate);
        }
        String hex = resolveColorHex(candidate);
        if (!hex.equals(candidate)) {
            return "§x" + convertToHexFormat(hex);
        }
        String legacy = colorNameToLegacy(candidate);
        if (!legacy.isEmpty()) {
            return legacy;
        }
        return null;
    }

    private static String processShortColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = SHORT_COLOR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String rawName = matcher.group(1); // e.g. "鹅黄显示模组列表" (greedy)
            String replacement = null;

            // Try progressively shorter prefixes to find the longest valid color name
            for (int len = rawName.length(); len >= 1; len--) {
                String candidate = rawName.substring(0, len);
                replacement = tryResolveColor(candidate);
                if (replacement != null) {
                    // Append any trailing text that wasn't part of the color name
                    if (len < rawName.length()) {
                        replacement += rawName.substring(len);
                    }
                    break;
                }
            }

            if (replacement == null) {
                replacement = matcher.group(0); // no valid color found, keep literal
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
