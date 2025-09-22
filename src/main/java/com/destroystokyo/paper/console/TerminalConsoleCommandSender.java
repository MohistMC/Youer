package com.destroystokyo.paper.console;

import com.mohistmc.youer.api.ColorAPI;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.logging.log4j.LogManager;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;

public class TerminalConsoleCommandSender extends CraftConsoleCommandSender {

    private static final ComponentLogger LOGGER = ComponentLogger.logger(LogManager.getRootLogger().getName());
    @Override
    public void sendRawMessage(String message) {
        final Component msg = ColorAPI.colorize(message);
        this.sendMessage(Identity.nil(), msg, MessageType.SYSTEM);
    }

    @Override
    public void sendMessage(Identity identity, Component message, MessageType type) {
        LOGGER.info(message);
    }

}
