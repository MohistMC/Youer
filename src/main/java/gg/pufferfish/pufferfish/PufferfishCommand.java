package gg.pufferfish.pufferfish;

import com.mohistmc.youer.util.I18n;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class PufferfishCommand extends Command {

    public PufferfishCommand() {
        super("pufferfish");
        this.description = "Pufferfish related commands";
        this.usageMessage = "/pufferfish [reload | version]";
        this.setPermission("bukkit.command.pufferfish");
    }
    
    public static void init() {
        MinecraftServer.getServer().server.getCommandMap().register("pufferfish", "Pufferfish", new PufferfishCommand());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length == 1) {
            return Stream.of("reload", "version")
              .filter(arg -> arg.startsWith(args[0].toLowerCase()))
              .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;
        String prefix = ChatColor.of("#12fff6") + "" + ChatColor.BOLD + "Pufferfish » " + ChatColor.of("#e8f9f9");

        if (args.length != 1) {
            sender.sendMessage(prefix + "Usage: " + usageMessage);
            args = new String[]{"version"};
        }

        if (args[0].equalsIgnoreCase("reload")) {
            MinecraftServer console = MinecraftServer.getServer();
            try {
                PufferfishConfig.load();
            } catch (IOException e) {
                sender.sendMessage(Component.text("Failed to reload.", NamedTextColor.RED));
                e.printStackTrace();
                return true;
            }
            console.server.reloadCount++;

            Command.broadcastCommandMessage(sender, prefix + "Pufferfish configuration has been reloaded.");
        } else if (args[0].equalsIgnoreCase("version")) {
            Command.broadcastCommandMessage(sender, prefix + I18n.as("server.version.message",
                    getName(),
                    Bukkit.getVersion(),
                    Bukkit.getBukkitVersion(),
                    NeoForgeVersion.getVersion()
            ));
        }

        return true;
    }
}
