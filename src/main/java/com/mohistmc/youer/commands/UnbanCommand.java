package com.mohistmc.youer.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.commands.PardonCommand;

/**
 * @author Mgazul
 * @date 2025/11/2 15:18
 */
public class UnbanCommand {

    public static void register(CommandDispatcher<CommandSourceStack> p_138094_) {
        p_138094_.register(
                Commands.literal("unban")
                        .requires(p_138101_ -> p_138101_.hasPermission(3))
                        .then(
                                Commands.argument("targets", GameProfileArgument.gameProfile())
                                        .suggests(
                                                (p_138098_, p_138099_) -> SharedSuggestionProvider.suggest(
                                                        p_138098_.getSource().getServer().getPlayerList().getBans().getUserList(), p_138099_
                                                )
                                        )
                                        .executes(p_138096_ -> PardonCommand.pardonPlayers(p_138096_.getSource(), GameProfileArgument.getGameProfiles(p_138096_, "targets")))
                        )
        );
    }
}
