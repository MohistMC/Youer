package com.mohistmc.youer;

import com.mohistmc.i18n.i18n;
import com.mohistmc.youer.eventhandler.EventDispatcherRegistry;
import com.mohistmc.youer.feature.ban.BanConfig;
import com.mohistmc.youer.feature.db.DatabaseConfig;
import com.mohistmc.youer.util.ExceptionHandler;
import java.util.Locale;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Youer {
    public static final String NAME = "Youer";
    public static final String modid = "youer";
    public static Logger LOGGER = LogManager.getLogger();
    public static i18n i18n;

    public Youer(IEventBus modEventBus, Dist dist, ModContainer container) {
        EventDispatcherRegistry.init();
        DatabaseConfig.init();
        BanConfig.init();
        if (i18n.isCN()) {
            new ExceptionHandler();
        }
    }

    public static void initI18n() {
        String mohist_lang = YouerConfig.yml.getString("youer.lang", Locale.getDefault().toString());
        i18n = new i18n(Youer.class.getClassLoader(), mohist_lang);
    }

    public static void initProperty() {
        System.setProperty("library.jansi.version", "Paper"); // Paper - set meaningless jansi version to prevent git builds from crashing on Windows
        System.setProperty("jdk.console", "java.base"); // Paper - revert default console provider back to java.base so we can have our own jline
        System.setProperty("java.util.logging.manager", "io.papermc.paper.log.CustomLogManager");
        if (System.getProperty("jdk.nio.maxCachedBufferSize") == null) System.setProperty("jdk.nio.maxCachedBufferSize", "262144"); // Paper - cap per-thread NIO cache size; https://www.evanjones.ca/java-bytebuffer-leak.html
    }
}