package com.mohistmc.youer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Mgazul
 * @date 2025/11/23 01:40
 */
public class Cooldown {
    private static Map<String, Cooldown> cooldowns;
    private final int timeInSeconds;
    private final UUID id;
    private final String cooldownName;
    private long start;

    public Cooldown(final UUID id, final String cooldownName, final int timeInSeconds) {
        this.id = id;
        this.cooldownName = cooldownName;
        this.timeInSeconds = timeInSeconds;
    }

    public static boolean isInCooldown(final UUID id, final String cooldownName) {
        if (getTimeLeft(id, cooldownName) >= 1) {
            return true;
        }
        stop(id, cooldownName);
        return false;
    }

    private static void stop(final UUID id, final String cooldownName) {
        Cooldown.cooldowns.remove(String.valueOf(id) + cooldownName);
    }

    private static Cooldown getCooldown(final UUID id, final String cooldownName) {
        return Cooldown.cooldowns.get(id.toString() + cooldownName);
    }

    public static int getTimeLeft(final UUID id, final String cooldownName) {
        final Cooldown cooldown = getCooldown(id, cooldownName);
        int f = -1;
        if (cooldown != null) {
            final long now = System.currentTimeMillis();
            final long cooldownTime = cooldown.start;
            final int totalTime = cooldown.timeInSeconds;
            final int r = (int)(now - cooldownTime) / 1000;
            f = (r - totalTime) * -1;
        }
        return f;
    }

    public void start() {
        this.start = System.currentTimeMillis();
        Cooldown.cooldowns.put(this.id.toString() + this.cooldownName, this);
    }

    static {
        Cooldown.cooldowns = new HashMap<String, Cooldown>();
    }
}
