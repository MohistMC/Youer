package org.purpurmc.purpur.task;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Bossbar for the pregen command, modeled on {@link TPSBarTask}: the shared
 * {@link BossBarTask} machinery handles per-tick updates, player removal on quit and
 * re-show on rejoin (via {@link BossBarTask#addToAll(net.minecraft.server.level.ServerPlayer)}).
 *
 * <p>Progress / color / title are pushed by the pregen ticker on the main thread;
 * {@link #updateBossBar(BossBar, Player)} renders them to every tracked player.
 *
 * @author Mgazul
 */
public class PregenBossBarTask extends BossBarTask {
    private static PregenBossBarTask instance;
    private float progress;
    private BossBar.Color color = BossBar.Color.GREEN;
    private Component title = Component.text("");
    private int tick = 0;
    // A cancelled BukkitRunnable cannot be re-scheduled (checkNotYetScheduled throws), so the
    // task starts exactly once for the server lifetime; per-task cleanup only removes players.
    private boolean startedOnce;

    public static PregenBossBarTask instance() {
        if (instance == null) {
            instance = new PregenBossBarTask();
        }
        return instance;
    }

    @Override
    public void start() {
        if (startedOnce) {
            return;
        }
        startedOnce = true;
        super.start();
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setColor(BossBar.Color color) {
        this.color = color;
    }

    /** Sets the bossbar title from a legacy color-code string (both {@code &} and {@code §} codes). */
    public void setTitle(String legacyTitle) {
        this.title = LegacyComponentSerializer.legacyAmpersand().deserialize(legacyTitle);
    }

    @Override
    BossBar createBossBar() {
        return BossBar.bossBar(Component.text(""), 0.0F, color, BossBar.Overlay.PROGRESS);
    }

    @Override
    void updateBossBar(BossBar bossbar, Player player) {
        bossbar.progress(progress);
        bossbar.color(color);
        bossbar.name(title);
    }

    @Override
    public void run() {
        if (++tick < 10) {
            return;
        }
        tick = 0;
        super.run();
    }
}
