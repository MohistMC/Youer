package org.bukkit;

/**
 * An Enum of categories for sounds.
 */
public enum SoundCategory implements com.mohistmc.net.kyori.adventure.sound.Sound.Source.Provider { // Paper - implement Sound.Source.Provider

    MASTER,
    MUSIC,
    RECORDS,
    WEATHER,
    BLOCKS,
    HOSTILE,
    NEUTRAL,
    PLAYERS,
    AMBIENT,
    VOICE;

    // Paper start - implement Sound.Source.Provider
    @Override
    public com.mohistmc.net.kyori.adventure.sound.Sound.@org.jetbrains.annotations.NotNull Source soundSource() {
        return switch (this) {
            case MASTER -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.MASTER;
            case MUSIC -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.MUSIC;
            case RECORDS -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.RECORD;
            case WEATHER -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.WEATHER;
            case BLOCKS -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.BLOCK;
            case HOSTILE -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.HOSTILE;
            case NEUTRAL -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.NEUTRAL;
            case PLAYERS -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.PLAYER;
            case AMBIENT -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.AMBIENT;
            case VOICE -> com.mohistmc.net.kyori.adventure.sound.Sound.Source.VOICE;
        };
    }
    // Paper end
}
