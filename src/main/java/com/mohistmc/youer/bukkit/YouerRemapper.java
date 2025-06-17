package com.mohistmc.youer.bukkit;

import java.util.Map;

public class YouerRemapper {
    private static final Map<String, String> MAPPINGS = loadMappings();

    public static String remapClassName(String original) {
        // 遍历映射表，找到最长的匹配前缀
        String bestMatch = null;
        for (Map.Entry<String, String> entry : MAPPINGS.entrySet()) {
            String prefix = entry.getKey();
            // 检查原始类名是否以当前映射前缀开头
            if (original.startsWith(prefix)) {
                // 选择最长的匹配前缀（避免短前缀覆盖长前缀）
                if (bestMatch == null || prefix.length() > bestMatch.length()) {
                    bestMatch = prefix;
                }
            }
        }

        // 找到匹配前缀时进行替换
        if (bestMatch != null) {
            String replacement = MAPPINGS.get(bestMatch);
            // 替换前缀并保留剩余部分
            return replacement + original.substring(bestMatch.length());
        }

        // 无匹配时返回原始类名
        return original;
    }

    private static Map<String, String> loadMappings() {
        // 实际应从mappings文件加载（如paper-remappings.tiny）
        // 示例返回静态映射表
        return Map.of("com.destroystokyo.paper.", "io.papermc.paper.");
    }
}
