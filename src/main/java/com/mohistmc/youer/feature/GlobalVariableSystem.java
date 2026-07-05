package com.mohistmc.youer.feature;

import com.mohistmc.youer.api.WorldAPI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 全局变量系统 — 受 PlaceholderAPI 架构启发重构
 *
 * <p>提供轻量级的文本变量替换功能，支持三种变量源：
 * <ul>
 *   <li><b>动态 Provider</b> — 通过 {@link #registerVariable(String, Function)} 注册的函数式变量，每次解析时实时计算</li>
 *   <li><b>全局变量</b> — 通过 {@link #setGlobalVariable(String, String)} 设置的静态全局值</li>
 *   <li><b>玩家变量</b> — 通过 {@link #setPlayerVariable(Player, String, String)} 设置的玩家私有值</li>
 * </ul>
 *
 * <p>变量引用格式：{@code %variable_name%}。
 * 对于包含下划线的变量名（如 {@code %player_name%}），支持可选的命名空间解析（ns_key），
 * 方便与 PlaceholderAPI 的格式兼容。
 *
 * <p>解析优先级（高→低）：玩家变量 → 全局变量 → 动态 Provider → 保留原文
 */
public class GlobalVariableSystem {

    private static final GlobalVariableSystem INSTANCE = new GlobalVariableSystem();

    private final Map<String, Function<Player, String>> variableProviders = new ConcurrentHashMap<>();
    private final Map<String, String> globalVariables = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> playerVariables = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    private GlobalVariableSystem() {
    }

    // ════════════════════════════════════════════════════════════
    //  Public Static API
    // ════════════════════════════════════════════════════════════

    /**
     * 初始化全局变量系统，注册所有内置变量。
     * 在服务器启动时由 {@link YouerPlugin#init()} 调用。
     */
    public static void register() {
        INSTANCE.initialize();
    }

    /**
     * 解析文本，将其中所有 {@code %variable_name%} 占位符替换为对应值。
     *
     * @param player 玩家上下文
     * @param input  要解析的文本
     * @return 替换后的文本；若 input 为 null 或空则返回空串
     */
    @NotNull
    public static String as(@Nullable Player player, @Nullable String input) {
        if (input == null || input.isEmpty()) return "";
        return INSTANCE.resolve(player, input);
    }

    /**
     * 解析文本，支持离线玩家上下文。
     *
     * @param player 离线玩家（可为 null）
     * @param input  要解析的文本
     * @return 替换后的文本；若 input 为 null 或空则返回空串
     */
    @NotNull
    public static String as(@Nullable OfflinePlayer player, @Nullable String input) {
        if (input == null || input.isEmpty()) return "";
        return INSTANCE.resolve(player, input);
    }

    /**
     * 批量解析文本列表。
     *
     * @param player 玩家上下文
     * @param inputs 文本列表
     * @return 替换后的文本列表
     */
    @NotNull
    public static List<String> as(@Nullable Player player, @Nullable List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) return Collections.emptyList();
        return INSTANCE.resolveAll(player, inputs);
    }

    // ─── Variable Registration & Management ──────────────────

    /**
     * 注册一个动态变量 Provider。
     * Provider 是一个函数，接收 Player 上下文并返回变量值。
     *
     * @param varName  变量名（不需要百分号，如 "player_name"）
     * @param provider 变量值提供函数；返回 null 表示该变量不适用
     */
    public static void registerVariable(@NotNull String varName,
                                        @NotNull Function<Player, String> provider) {
        INSTANCE.variableProviders.put(normalize(varName), provider);
    }

    /**
     * 注销一个已注册的动态变量。
     *
     * @param varName 变量名
     * @return 如果之前有注册则返回 true
     */
    public static boolean unregisterVariable(@NotNull String varName) {
        return INSTANCE.variableProviders.remove(normalize(varName)) != null;
    }

    /**
     * 设置一个全局变量（所有玩家共享）。
     *
     * @param key   变量名
     * @param value 变量值
     */
    public static void setGlobalVariable(@NotNull String key, @Nullable String value) {
        if (value != null) {
            INSTANCE.globalVariables.put(normalize(key), value);
        } else {
            INSTANCE.globalVariables.remove(normalize(key));
        }
    }

    /**
     * 获取一个全局变量的值。
     *
     * @param key 变量名
     * @return Optional 包裹的值，不存在则为空
     */
    @NotNull
    public static Optional<String> getGlobalVariable(@NotNull String key) {
        return Optional.ofNullable(INSTANCE.globalVariables.get(normalize(key)));
    }

    /**
     * 设置玩家私有变量（仅该玩家可见）。
     *
     * @param player 玩家
     * @param key    变量名
     * @param value  变量值
     */
    public static void setPlayerVariable(@NotNull Player player, @NotNull String key,
                                          @Nullable String value) {
        if (value != null) {
            INSTANCE.playerVariables
                    .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(normalize(key), value);
        } else {
            Map<String, String> pVars = INSTANCE.playerVariables.get(player.getUniqueId());
            if (pVars != null) {
                pVars.remove(normalize(key));
            }
        }
    }

    /**
     * 获取当前注册的所有动态变量名。
     */
    @NotNull
    public static Collection<String> getRegisteredVariables() {
        return Collections.unmodifiableSet(INSTANCE.variableProviders.keySet());
    }

    /**
     * 清空所有全局变量并重新初始化。
     */
    public static void reloadGlobalVariables() {
        INSTANCE.globalVariables.clear();
        INSTANCE.initGlobalVariables();
    }

    /**
     * 关闭系统，释放资源。
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "2.0.0")
    public static void shutdown() {
        // reserved: nothing to release
    }

    // ════════════════════════════════════════════════════════════
    //  Internal Initialization
    // ════════════════════════════════════════════════════════════

    private void initialize() {
        if (initialized) return;
        initialized = true;
        registerDefaultProviders();
        initGlobalVariables();
    }

    /**
     * 注册所有内置的动态变量 Provider。
     * 设计参考：PlaceholderAPI 的 PlaceholderExpansion#onRequest() 模式。
     */
    private void registerDefaultProviders() {
        registerVariable("player_name", Player::getName);
        registerVariable("player_displayname", Player::getDisplayName);

        registerVariable("player_health", p -> String.format(Locale.ROOT, "%.1f", p.getHealth()));
        registerVariable("player_max_health", p -> String.format(Locale.ROOT, "%.1f", p.getMaxHealth()));

        registerVariable("player_level", p -> String.valueOf(p.getLevel()));
        registerVariable("player_exp", p -> String.format(Locale.ROOT, "%.2f", p.getExp()));

        registerVariable("player_world", p -> WorldAPI.getWorldName(p.getWorld()));

        registerVariable("player_gamemode", p -> p.getGameMode().name());

        registerVariable("player_ip", p -> {
            try {
                return p.getAddress() != null
                        ? p.getAddress().getAddress().getHostAddress()
                        : "unknown";
            } catch (Exception ignored) {
                return "unknown";
            }
        });
    }

    /**
     * 初始化全局变量（同步执行，因为 register() 调用时服务器已就绪）。
     */
    private void initGlobalVariables() {
        try {
            globalVariables.put("server_name", Bukkit.getServer().getName());
            globalVariables.put("server_version", Bukkit.getServer().getVersion());
        } catch (Exception ignored) {
            globalVariables.put("server_name", "Youer Server");
            globalVariables.put("server_version", "Unknown");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Replacement Engine — inspired by PlaceholderAPI CharsReplacer
    // ════════════════════════════════════════════════════════════

    /**
     * 使用字符扫描方式替换文本中的所有变量占位符。
     *
     * <p>与 PlaceholderAPI 的 {@code CharsReplacer} 类似，逐字符扫描，
     * 避免了正则表达式的开销。支持 {@code %var%} 格式。
     *
     * @param player 玩家上下文
     * @param text   原始文本
     * @return 替换后的文本
     */
    @NotNull
    private String resolve(@Nullable Player player, @NotNull String text) {
        if (text.isEmpty()) return "";

        int start = text.indexOf('%');
        if (start == -1) {
            return text; // fast path: no placeholders
        }

        int length = text.length();
        StringBuilder result = new StringBuilder(length + (length >> 3)); // pre-allocate ~12.5% extra
        int cursor = 0;

        do {
            // append text before this placeholder
            if (start > cursor) {
                result.append(text, cursor, start);
            }

            // find closing %
            int end = text.indexOf('%', start + 1);
            if (end == -1) {
                // unclosed %, rest is plain text
                result.append(text, start, length);
                return result.toString();
            }

            // extract name and resolve
            String varName = text.substring(start + 1, end);
            String replacement = lookupValue(player, varName);

            if (replacement != null) {
                result.append(replacement);
            } else {
                // unknown: keep original %varName%
                result.append('%').append(varName).append('%');
            }

            cursor = end + 1;
            start = text.indexOf('%', cursor);

        } while (start != -1);

        // append trailing text
        if (cursor < length) {
            result.append(text, cursor, length);
        }

        return result.toString();
    }

    /**
     * 支持离线玩家的解析。
     */
    @NotNull
    private String resolve(@Nullable OfflinePlayer player, @NotNull String text) {
        if (player == null || !player.isOnline()) {
            // offline: global variables only
            return resolveOffline(text);
        }
        return resolve(player.getPlayer(), text);
    }

    /**
     * 离线解析：仅替换全局变量和不依赖 Player 的动态变量。
     */
    @NotNull
    private String resolveOffline(@NotNull String text) {
        if (text.isEmpty()) return "";

        int start = text.indexOf('%');
        if (start == -1) return text;

        int length = text.length();
        StringBuilder result = new StringBuilder(length + (length >> 3));
        int cursor = 0;

        do {
            if (start > cursor) {
                result.append(text, cursor, start);
            }

            int end = text.indexOf('%', start + 1);
            if (end == -1) {
                result.append(text, start, length);
                return result.toString();
            }

            String varName = text.substring(start + 1, end);
            String lowerKey = normalize(varName);

            // global-only lookup
            String replacement = globalVariables.get(lowerKey);
            if (replacement != null) {
                result.append(replacement);
            } else {
                result.append('%').append(varName).append('%');
            }

            cursor = end + 1;
            start = text.indexOf('%', cursor);

        } while (start != -1);

        if (cursor < length) {
            result.append(text, cursor, length);
        }
        return result.toString();
    }

    /**
     * 批量解析。
     */
    @NotNull
    private List<String> resolveAll(@Nullable Player player, @NotNull List<String> inputs) {
        List<String> result = new java.util.ArrayList<>(inputs.size());
        for (String input : inputs) {
            result.add(resolve(player, input));
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════
    //  Variable Lookup Engine
    // ════════════════════════════════════════════════════════════

    /**
     * Multi-level variable lookup inspired by PlaceholderAPI's layered architecture.
     *
     * <p>Lookup order:
     * <ol>
     *   <li>Player-specific variables (online players only)</li>
     *   <li>Global variables</li>
     *   <li>Dynamic providers</li>
     *   <li>Return null (caller retains original text)</li>
     * </ol>
     *
     * @param player  player context (may be null)
     * @param varName variable name (without percent signs)
     * @return variable value, or null if not found
     */
    @Nullable
    private String lookupValue(@Nullable Player player, @NotNull String varName) {
        String key = normalize(varName);

        // 1. Player variables
        if (player != null) {
            Map<String, String> pVars = playerVariables.get(player.getUniqueId());
            if (pVars != null) {
                String val = pVars.get(key);
                if (val != null) return val;
            }
        }

        // 2. Global variables
        String globalVal = globalVariables.get(key);
        if (globalVal != null) return globalVal;

        // 3. Dynamic providers
        if (player != null) {
            Function<Player, String> provider = variableProviders.get(key);
            if (provider != null) {
                try {
                    return provider.apply(player);
                } catch (Exception ignored) {
                    // provider exception = treat as not found
                }
            }
        }

        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  Utility Methods
    // ════════════════════════════════════════════════════════════

    /**
     * Normalize variable name: lowercase and trim.
     */
    @NotNull
    private static String normalize(@NotNull String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
