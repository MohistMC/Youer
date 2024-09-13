package com.mohistmc.youer;

import com.mohistmc.youer.eventhandler.EventDispatcherRegistry;
import com.mohistmc.i18n.i18n;
import com.mohistmc.youer.plugins.MohistProxySelector;
import com.mohistmc.youer.util.VersionInfo;
import java.net.ProxySelector;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.CraftServer;

@Mod("youer")
@OnlyIn(Dist.DEDICATED_SERVER)
public class Youer {
    public static final String NAME = "Youer";
    public static Logger LOGGER = LogManager.getLogger();
    public static i18n i18n;
    public static String version = "1.21.1";
    public static String modid = "youer";
    public static ClassLoader classLoader;
    public static VersionInfo versionInfo;

    public Youer() {
        classLoader = Youer.class.getClassLoader();

        //TODO: do something when mod loading
        LOGGER.info("Mohist mod loading.....");
        EventDispatcherRegistry.init();
        ProxySelector.setDefault(new MohistProxySelector(ProxySelector.getDefault()));
    }

    public static void initVersion() {
        String mohist_lang = MohistConfig.yml.getString("youer.lang", Locale.getDefault().toString());
        i18n = new i18n(Youer.class.getClassLoader(), mohist_lang);

        Map<String, String> arguments = new HashMap<>();
        String[] cbs = CraftServer.class.getPackage().getImplementationVersion().split("-");
        arguments.put("mohist", (Youer.class.getPackage().getImplementationVersion() != null) ? Youer.class.getPackage().getImplementationVersion() : version);
        arguments.put("bukkit", cbs[0]);
        arguments.put("craftbukkit", cbs[1]);
        arguments.put("spigot", cbs[2]);
        arguments.put("neoforge", cbs[3]);
        arguments.put("forge", NeoForgeVersion.getVersion());
        versionInfo = new VersionInfo(arguments);
    }
}