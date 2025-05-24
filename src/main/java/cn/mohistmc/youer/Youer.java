package cn.mohistmc.youer;

import cn.mohistmc.youer.eventhandler.EventDispatcherRegistry;
import cn.mohistmc.youer.plugins.MohistProxySelector;
import cn.mohistmc.youer.util.VersionInfo;
import com.mohistmc.i18n.i18n;
import java.io.File;
import java.net.ProxySelector;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

@Mod("youer")
@OnlyIn(Dist.DEDICATED_SERVER)
public class Youer {
    public static final String NAME = "Youer";
    public static Logger LOGGER = LogManager.getLogger();
    public static i18n i18n;
    public static String version = "1.21.1";
    public static final String modid = "youer";
    public static ClassLoader classLoader;
    public static VersionInfo versionInfo;

    public Youer(IEventBus modEventBus, Dist dist, ModContainer container) {
        classLoader = Youer.class.getClassLoader();

        //TODO: do something when mod loading
        LOGGER.info("Youer mod loading.....");
        //EventDispatcherRegistry.init();
        ProxySelector.setDefault(new MohistProxySelector(ProxySelector.getDefault()));
        File CONFIG_FILE = new File("youer.yml");
        try {
            if (!CONFIG_FILE.exists()) {
                System.out.println("Youer config file not found, creating new one...");
                CONFIG_FILE.createNewFile();
            }
        } catch (Exception e) {
            System.out.println("File init exception!");
        }
        YouerConfig.config = YamlConfiguration.loadConfiguration(CONFIG_FILE);
        String mohist_lang = YouerConfig.config.getString("youer.lang", Locale.getDefault().toString());
        i18n = new i18n(Youer.class.getClassLoader(), mohist_lang);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("youer", version);
        arguments.put("bukkit", version);
        arguments.put("craftbukkit", version);
        arguments.put("spigot", version);
        arguments.put("neoforge", NeoForgeVersion.getVersion());
        versionInfo = new VersionInfo(arguments);
        EventDispatcherRegistry.init();
    }
}