package gg.pufferfish.pufferfish;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

public class PufferfishLogger extends Logger {
    public static final PufferfishLogger LOGGER = new PufferfishLogger();

    private PufferfishLogger() {
        super("Pufferfish", null);

        setParent(Bukkit.getLogger());
        setLevel(Level.ALL);
    }
}
