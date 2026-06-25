package com.destroystokyo.paper.console;

import com.mohistmc.youer.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

public class TerminalConsoleCommandSender extends CraftConsoleCommandSender {

    private static final Logger LOGGER = LogManager.getLogger(TerminalConsoleCommandSender.class);

    @Override
    public void sendRawMessage(String message) {
        LOGGER.info(ColorAPI.ansi(message));
    }

    @Override
    public void sendMessage(final @NotNull Component message) {
        LOGGER.info(ColorAPI.ansi(message));
    }

}
