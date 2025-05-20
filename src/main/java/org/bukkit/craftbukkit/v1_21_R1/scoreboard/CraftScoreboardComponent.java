package org.bukkit.craftbukkit.v1_21_R1.scoreboard;

abstract class CraftScoreboardComponent {
    private CraftScoreboard scoreboard;

    CraftScoreboardComponent(CraftScoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    abstract CraftScoreboard checkState();

    public CraftScoreboard getScoreboard() {
        return this.scoreboard;
    }

    abstract void unregister();
}
