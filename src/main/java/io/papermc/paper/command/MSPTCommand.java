package io.papermc.paper.command;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import com.mohistmc.youer.util.I18n;

@DefaultQualifier(NonNull.class)
public final class MSPTCommand extends Command {
    private static final DecimalFormat DF = new DecimalFormat("########0.0");
    private static final Component SLASH = text("/");

    public MSPTCommand(final String name) {
        super(name);
        this.description = I18n.as("msptcmd.description");
        this.usageMessage = "/mspt";
        this.setPermission("bukkit.command.mspt");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;

        MinecraftServer server = MinecraftServer.getServer();

        List<Component> times = new ArrayList<>();
        times.addAll(eval(server.tickTimes5s.getTimes()));
        times.addAll(eval(server.tickTimes10s.getTimes()));
        times.addAll(eval(server.tickTimes60s.getTimes()));

        sender.sendMessage(text().content(I18n.as("msptcmd.title")).color(GOLD)
                .append(text().color(YELLOW)
                        .append(
                                text("("),
                                text(I18n.as("msptcmd.avg"), GRAY),
                                text("/"),
                                text(I18n.as("msptcmd.min"), GRAY),
                                text("/"),
                                text(I18n.as("msptcmd.max"), GRAY),
                                text(")")
                        )
                ).append(
                        text(" " + I18n.as("msptcmd.periods"))
                )
        );
        sender.sendMessage(text().content(I18n.as("msptcmd.symbol") + " ").color(GOLD)
                .append(text().color(GRAY)
                        .append(
                                times.get(0), SLASH, times.get(1), SLASH, times.get(2), text(", ", YELLOW),
                                times.get(3), SLASH, times.get(4), SLASH, times.get(5), text(", ", YELLOW),
                                times.get(6), SLASH, times.get(7), SLASH, times.get(8)
                        )
                )
        );
        return true;
    }

    private static List<Component> eval(long[] times) {
        long min = Integer.MAX_VALUE;
        long max = 0L;
        long total = 0L;
        for (long value : times) {
            if (value > 0L && value < min) min = value;
            if (value > max) max = value;
            total += value;
        }
        double avgD = ((double) total / (double) times.length) * 1.0E-6D;
        double minD = ((double) min) * 1.0E-6D;
        double maxD = ((double) max) * 1.0E-6D;
        return Arrays.asList(getColor(avgD), getColor(minD), getColor(maxD));
    }

    private static Component getColor(double avg) {
        return text(DF.format(avg), avg >= 50 ? RED : avg >= 40 ? YELLOW : GREEN);
    }
}
