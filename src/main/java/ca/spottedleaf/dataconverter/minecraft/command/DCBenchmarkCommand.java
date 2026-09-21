package ca.spottedleaf.dataconverter.minecraft.command;

import ca.spottedleaf.dataconverter.util.ConvertUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.dedicated.DedicatedServer;

import static net.minecraft.commands.Commands.literal;

public final class DCBenchmarkCommand {

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("dcbenchmark").requires((final CommandSourceStack src) -> {
                return Commands.hasPermission(Commands.LEVEL_ADMINS).test(src) || !(src.getServer() instanceof DedicatedServer);
            }).then(literal("reset")
                .executes(DCBenchmarkCommand::reset)
            )
        );
    }

    public static int reset(final CommandContext<CommandSourceStack> ctx) {
        ConvertUtil.resetAndPrintBenchmark();
        return 0;
    }
}
