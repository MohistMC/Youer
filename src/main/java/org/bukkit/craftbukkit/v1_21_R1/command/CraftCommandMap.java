package org.bukkit.craftbukkit.v1_21_R1.command;

import java.util.Map;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

public class CraftCommandMap extends SimpleCommandMap {

    public CraftCommandMap(Server server) {
        super(server);
    }

    public Map<String, Command> getKnownCommands() {
        return this.knownCommands;
    }
}
