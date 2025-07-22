package com.mohistmc.youer.feature;

import com.mohistmc.youer.api.WorldAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GlobalVariableSystem {
    // 单例实例
    @Getter
    public static GlobalVariableSystem instance = new GlobalVariableSystem();

    private final Map<String, Function<Player, String>> variableProviders = new ConcurrentHashMap<>();
    private final Map<String, String> globalVariables = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> playerVariables = new ConcurrentHashMap<>();
    private final Map<String, String> parsedCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 2000;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("%([^%]+)%");

    /**
     * 注册默认玩家变量palyer
     */
    public void registerDefaultPlayerVariables() {
        // 注册内置玩家变量
        registerVariable("player_name", Player::getName);
        registerVariable("player_displayname", Player::getDisplayName);
        registerVariable("player_health", p -> String.format("%.1f", p.getHealth()));
        registerVariable("player_max_health", p -> String.format("%.1f", p.getMaxHealth()));
        registerVariable("player_level", p -> String.valueOf(p.getLevel()));
        registerVariable("player_exp", p -> String.format("%.2f", p.getExp()));
        registerVariable("player_world", p -> p.getWorld().getName());
        registerVariable("player_world_name", p -> WorldAPI.getWorldName(p.getWorld()));
        registerVariable("player_gamemode", p -> p.getGameMode().name());
        registerVariable("player_ip", p -> p.getAddress().getAddress().getHostAddress());

        // 注册全局变量
        globalVariables.put("server_name", Bukkit.getServer().getName());
        globalVariables.put("server_version", Bukkit.getServer().getVersion());
    }

    /**
     * 注册变量提供器
     * @param varName 变量名(带百分号格式，如"player_name")
     * @param provider 变量提供函数
     */
    public void registerVariable(String varName, Function<Player, String> provider) {
        variableProviders.put(varName.toLowerCase(), provider);
    }

    /**
     * 设置全局变量
     * @param key 变量名(不带百分号)
     * @param value 变量值
     */
    public void setGlobalVariable(String key, String value) {
        globalVariables.put(key.toLowerCase(), value);
        clearCache();
    }

    /**
     * 设置玩家特定变量
     * @param player 玩家
     * @param key 变量名(不带百分号)
     * @param value 变量值
     */
    public void setPlayerVariable(Player player, String key, String value) {
        playerVariables.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(key.toLowerCase(), value);
        clearCache();
    }

    /**
     * 解析字符串中的变量(需要玩家上下文)
     * @param player 玩家
     * @param input 输入字符串
     * @return 解析后的字符串
     */
    public String parse(Player player, String input) {
        if (input == null || input.isEmpty()) return "";

        String cacheKey = player.getUniqueId() + "|" + input;
        if (parsedCache.containsKey(cacheKey)) {
            return parsedCache.get(cacheKey);
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(input);

        while (matcher.find()) {
            String varName = matcher.group(1).toLowerCase();
            String replacement = resolveVariable(player, varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        String output = result.toString();
        if (parsedCache.size() < MAX_CACHE_SIZE) {
            parsedCache.put(cacheKey, output);
        }

        return output;
    }

    private String resolveVariable(Player player, String varName) {
        // 1. 检查玩家特定变量
        if (playerVariables.containsKey(player.getUniqueId())) {
            String playerValue = playerVariables.get(player.getUniqueId()).get(varName);
            if (playerValue != null) return playerValue;
        }

        // 2. 检查全局变量
        String globalValue = globalVariables.get(varName);
        if (globalValue != null) return globalValue;

        // 3. 检查注册的变量提供器
        Function<Player, String> provider = variableProviders.get(varName);
        if (provider != null) {
            return provider.apply(player);
        }

        // 4. 未知变量保持原样
        return "%" + varName + "%";
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        parsedCache.clear();
    }

    /**
     * 重新加载全局变量
     */
    public void reloadGlobalVariables() {
        globalVariables.clear();
        globalVariables.put("server_name", Bukkit.getServer().getName());
        globalVariables.put("server_version", Bukkit.getServer().getVersion());
        clearCache();
    }
}
