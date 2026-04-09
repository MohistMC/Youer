package com.mohistmc.youer.feature.back;

public enum BackType {
    TELEPORT, DEATH;

    public boolean isTeleport() {
        return this == TELEPORT;
    }
}
