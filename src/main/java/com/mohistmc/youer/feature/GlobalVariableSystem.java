package com.mohistmc.youer.feature;

import com.mohistmc.youer.api.WorldAPI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GlobalVariableSystem {
    public static final GlobalVariableSystem instance = new GlobalVariableSystem();
    private static final int MAX_CACHE_SIZE = 2000;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("%([^%]+)%");
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Function<Player, String>> variableProviders = new ConcurrentHashMap<>();
    private final Map<String, String> globalVariables = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> playerVariables = new ConcurrentHashMap<>();
    private final Map<String, String> parsedCache = new ConcurrentHashMap<>();

    public static void register() {
        instance.registerDefaultPlayerVariables();
    }

    public static String as(Player player, String input) {
        return instance.parse(player, input);
    }

    /**
     * Shutdown the executor service
     */
    public static void shutdown() {
        executorService.shutdown();
    }

    /**
     * Register default player variables
     */
    private void registerDefaultPlayerVariables() {
        // Register built-in player variables
        registerVariable("player_name", Player::getName);
        registerVariable("player_displayname", Player::getDisplayName);
        registerVariable("player_health", p -> String.format("%.1f", p.getHealth()));
        registerVariable("player_max_health", p -> String.format("%.1f", p.getMaxHealth()));
        registerVariable("player_level", p -> String.valueOf(p.getLevel()));
        registerVariable("player_exp", p -> String.format("%.2f", p.getExp()));
        registerVariable("player_world", p -> WorldAPI.getWorldName(p.getWorld()));
        registerVariable("player_world_name", p -> WorldAPI.getWorldName(p.getWorld()));
        registerVariable("player_gamemode", p -> p.getGameMode().name());
        registerVariable("player_ip", p -> p.getAddress() != null ?
                p.getAddress().getAddress().getHostAddress() : "unknown");

        // Initialize global variables asynchronously
        initializeGlobalVariables();
    }

    /**
     * Initialize global variables asynchronously to prevent client lag
     */
    private void initializeGlobalVariables() {
        // Run server-related initialization in a separate thread to avoid blocking
        executorService.submit(() -> {
            try {
                // Add a small delay to ensure server is ready
                Thread.sleep(1000);
                // Initialize global variables in background
                globalVariables.put("server_name", Bukkit.getServer().getName());
                globalVariables.put("server_version", Bukkit.getServer().getVersion());
            } catch (Exception ignored) {
                // Fallback values in case of exception
                globalVariables.put("server_name", "Youer Server");
                globalVariables.put("server_version", "Unknown");
            }
        });
    }

    /**
     * Register variable provider
     *
     * @param varName  Variable name (without percent signs, e.g. "player_name")
     * @param provider Variable provider function
     */
    public void registerVariable(String varName, Function<Player, String> provider) {
        variableProviders.put(varName.toLowerCase(), provider);
    }

    /**
     * Set global variable
     *
     * @param key   Variable key (without percent signs)
     * @param value Variable value
     */
    public void setGlobalVariable(String key, String value) {
        globalVariables.put(key.toLowerCase(), value);
        clearCache();
    }

    /**
     * Set player-specific variable
     *
     * @param player Player instance
     * @param key    Variable key (without percent signs)
     * @param value  Variable value
     */
    public void setPlayerVariable(Player player, String key, String value) {
        playerVariables.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(key.toLowerCase(), value);
        clearCache();
    }

    /**
     * Parse variables in string (requires player context)
     *
     * @param player Player context
     * @param input  Input string
     * @return Parsed string
     */
    public String parse(Player player, String input) {
        if (input == null || input.isEmpty()) return "";

        // Create cache key
        String cacheKey = player.getUniqueId() + "|" + input;

        // Return cached result if available
        if (parsedCache.containsKey(cacheKey)) {
            return parsedCache.get(cacheKey);
        }

        // Parse variables
        StringBuilder result = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        boolean hasVariables = matcher.find();

        if (!hasVariables) {
            // No variables to parse, return original input
            return input;
        }

        // Reset matcher for actual parsing
        matcher.reset();

        while (matcher.find()) {
            String varName = matcher.group(1).toLowerCase();
            String replacement = resolveVariable(player, varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement : "null"));
        }
        matcher.appendTail(result);

        String output = result.toString();

        // Cache result if cache is not full
        if (parsedCache.size() < MAX_CACHE_SIZE) {
            parsedCache.put(cacheKey, output);
        }

        return output;
    }

    /**
     * Resolve variable value
     *
     * @param player  Player context
     * @param varName Variable name
     * @return Resolved variable value
     */
    private String resolveVariable(Player player, String varName) {
        // 1. Check player-specific variables
        if (playerVariables.containsKey(player.getUniqueId())) {
            String playerValue = playerVariables.get(player.getUniqueId()).get(varName);
            if (playerValue != null) return playerValue;
        }

        // 2. Check global variables
        String globalValue = globalVariables.get(varName);
        if (globalValue != null) return globalValue;

        // 3. Check registered variable providers
        Function<Player, String> provider = variableProviders.get(varName);
        if (provider != null) {
            try {
                return provider.apply(player);
            } catch (Exception e) {
                return "<error:" + varName + ">";
            }
        }

        // 4. Return original variable placeholder for unknown variables
        return "%" + varName + "%";
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        parsedCache.clear();
    }

    /**
     * Reload global variables
     */
    public void reloadGlobalVariables() {
        globalVariables.clear();
        initializeGlobalVariables();
        clearCache();
    }
}
