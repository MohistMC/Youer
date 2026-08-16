package com.mohistmc.youer.feature.logfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.Message;

/**
 * A Log4j2 filter that drops log messages matching any configured regex pattern.
 *
 * <p>Inspired by the console-spam-fix plugin: the server operator maintains a list
 * of regular expressions and every log line matching one of them is silently
 * discarded before it reaches the console, latest.log and debug.log.</p>
 *
 * <p>Note that the filter matches against the raw log message, i.e. the text
 * produced by the logger itself without the {@code [HH:mm:ss LEVEL]:} prefix that
 * the layout adds for display.</p>
 */
public final class LogFilter extends AbstractFilter {

    private static final Logger LOGGER = LogManager.getLogger("LogFilter");

    /** Single instance mounted on the root logger. */
    public static final LogFilter INSTANCE = new LogFilter();

    private volatile List<Pattern> patterns = new ArrayList<>();
    private volatile boolean enabled = true;

    private LogFilter() {
    }

    /**
     * Mount this filter on the root logger. Safe to call repeatedly: any
     * previously installed instance is removed first, so /youer reload does
     * not stack duplicate filters.
     */
    public static void register() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration conf = ctx.getConfiguration();
        LoggerConfig root = conf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        root.removeFilter(INSTANCE);
        root.addFilter(INSTANCE);
        ctx.updateLoggers(conf);
    }

    /**
     * Compile the configured rules. Invalid patterns are skipped so one typo
     * cannot disable the whole filter, but a warning is logged so the operator
     * can spot misconfigured rules instead of silently losing them.
     *
     * @param rules raw regex strings from the config file
     */
    public void setRules(List<String> rules) {
        List<Pattern> compiled = new ArrayList<>();
        if (rules != null) {
            for (String rule : rules) {
                try {
                    compiled.add(Pattern.compile(rule));
                } catch (Exception e) {
                    LOGGER.warn("LogFilter: skipping invalid rule \"{}\": {}", rule, e.getMessage());
                }
            }
        }
        this.patterns = compiled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    /**
     * The single hot path for every log event passing through the root logger.
     *
     * <p>Fast short-circuit: when filtering is disabled or no rules are compiled
     * the event passes through untouched and the message is never formatted,
     * keeping the overhead negligible even at high log rates.</p>
     */
    @Override
    public Result filter(LogEvent event) {
        if (event == null || !enabled || patterns.isEmpty()) {
            return Result.NEUTRAL;
        }
        Message message = event.getMessage();
        String msg = message == null ? null : message.getFormattedMessage();
        if (msg == null) {
            return Result.NEUTRAL;
        }
        // Command feedback (e.g. /logfilter list output) must never be filtered
        // by the very rules it displays - a "Leaked resource" rule must not hide
        // the list entry that shows "Leaked resource". Such feedback carries §
        // colour codes when sent to players, and ANSI escape sequences (ESC, 0x1B)
        // when sent to the console (CraftConsoleCommandSender converts § via the
        // ANSI serializer before logging), while raw server/mod log lines carry
        // neither. Skip both forms so a broad rule cannot swallow command output.
        if (msg.indexOf('§') >= 0 || msg.indexOf('\u001B') >= 0) {
            return Result.NEUTRAL;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(msg).find()) {
                return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }
}
