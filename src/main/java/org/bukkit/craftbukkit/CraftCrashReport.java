package org.bukkit.craftbukkit;

import com.mohistmc.youer.util.I18n;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

public class CraftCrashReport implements Supplier<String> {

    @Override
    public String get() {
        final io.papermc.paper.ServerBuildInfo build = io.papermc.paper.ServerBuildInfo.buildInfo(); // Paper
        if (Bukkit.getServer() == null) {
            return I18n.as("craftcrashreport.notRunning");
        }
        StringWriter value = new StringWriter();
        try {
            value.append("\n   ").append(I18n.as("craftcrashreport.brandInfo")).append(": ").append(String.format("%s (%s) %s %s", build.brandName(), build.brandId(), I18n.as("craftcrashreport.version"), build.asString(io.papermc.paper.ServerBuildInfo.StringRepresentation.VERSION_FULL))); // Paper
            value.append("\n   ").append(I18n.as("craftcrashreport.running")).append(": ").append(Bukkit.getName()).append(' ').append(I18n.as("craftcrashreport.version")).append(' ').append(Bukkit.getVersion()).append(" (").append(I18n.as("craftcrashreport.implementingApi")).append(' ').append(Bukkit.getBukkitVersion()).append(") ").append(String.valueOf(MinecraftServer.getServer().usesAuthentication()));
            value.append("\n   ").append(I18n.as("craftcrashreport.plugins")).append(": {");
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                PluginDescriptionFile description = plugin.getDescription();
                boolean legacy = CraftMagicNumbers.isLegacy(description);
                value.append("\n      ").append(description.getFullName()).append(legacy ? "*" : "")
                        .append(" (").append(description.getMain()).append(")")
                        .append(" authors=").append(Arrays.toString(description.getAuthors().toArray()));
            }
            value.append("\n   }");
            value.append("\n   ").append(I18n.as("craftcrashreport.warnings")).append(": ").append(Bukkit.getWarningState().name());
            value.append("\n   ").append(I18n.as("craftcrashreport.reloadCount")).append(": ").append(String.valueOf(MinecraftServer.getServer().server.reloadCount));
            value.append("\n   ").append(I18n.as("craftcrashreport.threads")).append(":");
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                Thread thread = entry.getKey();
                value.append("\n      ").append(thread.getState().name()).append(' ').append(thread.getName())
                        .append(" (id=").append(String.valueOf(thread.threadId())).append(", daemon=").append(String.valueOf(thread.isDaemon())).append("):");
                for (StackTraceElement element : entry.getValue()) {
                    value.append("\n         at ").append(String.valueOf(element));
                }
            }
            value.append("\n   ").append(Bukkit.getScheduler().toString());
            value.append("\n   ").append(I18n.as("craftcrashreport.forceLoadedChunks")).append(": {");
            for (World world : Bukkit.getWorlds()) {
                value.append(' ').append(world.getName()).append(": {");
                for (Map.Entry<Plugin, Collection<Chunk>> entry : world.getPluginChunkTickets().entrySet()) {
                    value.append(' ').append(entry.getKey().getDescription().getFullName()).append(": ").append(Integer.toString(entry.getValue().size())).append(',');
                }
                value.append("},");
            }
            value.append("}");
        } catch (Throwable t) {
            value.append("\n   ").append(I18n.as("craftcrashreport.failedToHandle")).append("\n");
            PrintWriter writer = new PrintWriter(value);
            t.printStackTrace(writer);
            writer.flush();
        }
        return value.toString();
    }

}
