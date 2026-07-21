package org.purpurmc.purpur.task;

import com.mohistmc.youer.feature.PacketStatistics;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

public class PacketBarTask extends BossBarTask {
    private static PacketBarTask instance;
    private long bytesPerSecond = 0L;
    private long packetsPerSecond = 0L;
    private long totalBytes = 0L;
    private long totalPackets = 0L;
    private int tick = 0;
    private boolean statsStarted = false;

    // --- Configurable settings (can be moved to YouerConfig / PurpurConfig) ---
    /** MiniMessage title template. Placeholders: <speed>, <packets>, <total_bytes>, <total_packets>, <percent> */
    public static String title = "<gray>Traffic<yellow>:</yellow> <speed> <gray>|</gray> <packets> pkt/s";

    /** BossBar overlay style */
    public static BossBar.Overlay overlay = BossBar.Overlay.NOTCHED_20;

    /** Color when traffic is below 50% of maxBytesPerSecond */
    public static BossBar.Color colorGood = BossBar.Color.GREEN;
    /** Color when traffic is between 50% and 75% */
    public static BossBar.Color colorMedium = BossBar.Color.YELLOW;
    /** Color when traffic is above 75% */
    public static BossBar.Color colorLow = BossBar.Color.RED;

    /** The traffic rate (bytes/s) that fills the progress bar to 100% */
    public static long maxBytesPerSecond = 10_000_000L; // 10 MB/s

    /** How many ticks between data refreshes (default 20 = 1 second) */
    public static int tickInterval = 20;

    public static PacketBarTask instance() {
        if (instance == null) {
            instance = new PacketBarTask();
        }
        return instance;
    }

    @Override
    BossBar createBossBar() {
        return BossBar.bossBar(Component.text(""), 0.0F, colorGood, overlay);
    }

    @Override
    void updateBossBar(BossBar bossbar, Player player) {
        bossbar.progress(getBossBarProgress());
        bossbar.color(getBossBarColor());
        bossbar.name(MiniMessage.miniMessage().deserialize(title,
                Placeholder.component("speed", formatTrafficSpeed(this.bytesPerSecond)),
                Placeholder.unparsed("packets", String.valueOf(this.packetsPerSecond)),
                Placeholder.component("total_bytes", formatBytes(this.totalBytes)),
                Placeholder.unparsed("total_packets", String.valueOf(this.totalPackets)),
                Placeholder.unparsed("percent", ((int) (getBossBarProgress() * 100F)) + "%")
        ));
    }

    @Override
    public void run() {
        // Auto-start packet statistics collection if not already active
        if (!this.statsStarted) {
            if (!PacketStatistics.isCollecting()) {
                PacketStatistics.startCollecting();
            }
            this.statsStarted = true;
        }

        if (++this.tick < tickInterval) {
            return;
        }
        this.tick = 0;

        this.bytesPerSecond = PacketStatistics.getBytesPerSecond();
        this.packetsPerSecond = PacketStatistics.getPacketsPerSecond();
        this.totalBytes = PacketStatistics.getTotalBytesSent();
        this.totalPackets = PacketStatistics.getTotalPacketsSent();

        super.run();
    }

    private float getBossBarProgress() {
        if (maxBytesPerSecond <= 0L) return 0.0F;
        return Math.clamp((float) this.bytesPerSecond / (float) maxBytesPerSecond, 0.0F, 1.0F);
    }

    private BossBar.Color getBossBarColor() {
        float progress = getBossBarProgress();
        if (progress < 0.5F) {
            return colorGood;
        } else if (progress < 0.75F) {
            return colorMedium;
        } else {
            return colorLow;
        }
    }

    // -- Format helpers --

    /** Format a byte count into human-readable form (e.g. "15.2 MB") */
    public static Component formatBytes(long bytes) {
        return Component.text(formatBytesRaw(bytes));
    }

    /** Format a byte count as a speed string with "/s" suffix (e.g. "1.5 MB/s") */
    public static Component formatTrafficSpeed(long bytesPerSec) {
        return Component.text(formatBytesRaw(bytesPerSec) + "/s");
    }

    private static String formatBytesRaw(long bytes) {
        if (bytes < 1024L) {
            return bytes + "B";
        }
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f%s", (double) bytes / (1L << (z * 10)), "BKMGTPE".charAt(z));
    }

    @Override
    public void stop() {
        super.stop();
        this.statsStarted = false;
    }

    // -- Accessors --

    public long getBytesPerSecond() {
        return this.bytesPerSecond;
    }

    public long getPacketsPerSecond() {
        return this.packetsPerSecond;
    }

    public long getTotalBytes() {
        return this.totalBytes;
    }

    public long getTotalPackets() {
        return this.totalPackets;
    }
}
