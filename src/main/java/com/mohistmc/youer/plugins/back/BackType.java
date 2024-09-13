package com.mohistmc.youer.plugins.back;

public enum BackType {
    TELEPORT,DEATH;

    public boolean isTeleport() {
        return this == TELEPORT;
    }
}
