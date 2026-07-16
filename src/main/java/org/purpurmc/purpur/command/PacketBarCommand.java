package org.purpurmc.purpur.command;

import com.mojang.brigadier.CommandDispatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.purpurmc.purpur.task.PacketBarTask;

import java.util.Collection;
import java.util.Collections;

public class PacketBarCommand {
    private static final String OUTPUT = "<gray>PacketBar</gray> <yellow>»</yellow> <onoff> <gray>for</gray> <target>";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("packetbar")
                .requires(listener -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.packetbar"))
                .executes(context -> execute(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(listener -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.packetbar.other"))
                        .executes(context -> execute(context.getSource(), EntityArgument.getPlayers(context, "targets")))
                )
        );
    }

    private static int execute(CommandSourceStack sender, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            boolean result = PacketBarTask.instance().togglePlayer(player.getBukkitEntity());
            player.packetBar(result);

            Component output = MiniMessage.miniMessage().deserialize(OUTPUT,
                    Placeholder.component("onoff", Component.translatable(result ? "options.on" : "options.off")
                            .color(result ? NamedTextColor.GREEN : NamedTextColor.RED)),
                    Placeholder.parsed("target", player.getGameProfile().name()));

            sender.sendSuccess(output, false);
        }
        return targets.size();
    }
}
