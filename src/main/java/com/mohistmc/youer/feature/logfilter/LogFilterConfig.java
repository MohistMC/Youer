package com.mohistmc.youer.feature.logfilter;

import com.mohistmc.youer.feature.config.YouerPluginConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Log4j2 log filter (logfilter.yml).
 *
 * <p>Stores the enable flag and the list of regex rules that {@link LogFilter}
 * uses to drop spammy log messages. Rules are edited directly in the file and
 * applied with the /logfilter reload command (or on server start).</p>
 */
public class LogFilterConfig extends YouerPluginConfig {

    public static final String FILTERS_KEY = "filters";
    public static final String ENABLE_KEY = "enable";

    public static LogFilterConfig INSTANCE;

    public LogFilterConfig(File file) {
        super(file);
    }

    public static void init() {
        INSTANCE = new LogFilterConfig(new File(YouerPluginConfig.CONFIG_FILE, "logfilter.yml"));
        if (!INSTANCE.yaml.contains(ENABLE_KEY)) {
            INSTANCE.yaml.set(ENABLE_KEY, false);
        }
        if (!INSTANCE.yaml.contains(FILTERS_KEY)) {
            INSTANCE.yaml.set(FILTERS_KEY, new ArrayList<>());
        }
        INSTANCE.save();
        INSTANCE.apply();
    }

    /** Push the current file contents into the mounted {@link LogFilter}. */
    public void apply() {
        LogFilter.INSTANCE.setEnabled(isEnabled());
        LogFilter.INSTANCE.setRules(getFilters());
    }

    public boolean isEnabled() {
        return yaml.getBoolean(ENABLE_KEY, false);
    }

    public void setEnabled(boolean enabled) {
        yaml.set(ENABLE_KEY, enabled);
        save();
        LogFilter.INSTANCE.setEnabled(enabled);
    }

    public List<String> getFilters() {
        return yaml.getStringList(FILTERS_KEY);
    }
}
