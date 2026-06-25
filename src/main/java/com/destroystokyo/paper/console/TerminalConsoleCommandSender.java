package com.destroystokyo.paper.console;

import com.mohistmc.youer.Youer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.logging.log4j.LogManager;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

public class TerminalConsoleCommandSender extends CraftConsoleCommandSender {

    private static final ComponentLogger LOGGER = ComponentLogger.logger(LogManager.getRootLogger().getName());

    @Override
    public void sendRawMessage(String message) {
        Youer.LOGGER.info(ChatColor.stripColor(message));
    }

    @Override
    public void sendMessage(final @NotNull Component message) {
        LOGGER.info(message);
    }

}
