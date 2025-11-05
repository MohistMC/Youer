package com.mohistmc.youer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.commands.PardonIpCommand;

/**
 * @author Mgazul
 * @date 2025/11/2 15:18
 */
public class UnBanIpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> p_138109_) {
        p_138109_.register(
                Commands.literal("unban-ip")
                        .requires(p_138116_ -> p_138116_.hasPermission(3))
                        .then(
                                Commands.argument("target", StringArgumentType.word())
                                        .suggests(
                                                (p_138113_, p_138114_) -> SharedSuggestionProvider.suggest(
                                                        p_138113_.getSource().getServer().getPlayerList().getIpBans().getUserList(), p_138114_
                                                )
                                        )
                                        .executes(p_138111_ -> PardonIpCommand.unban(p_138111_.getSource(), StringArgumentType.getString(p_138111_, "target")))
                        )
        );
    }
}
