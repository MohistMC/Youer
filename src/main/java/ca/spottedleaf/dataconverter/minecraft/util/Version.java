package ca.spottedleaf.dataconverter.minecraft.util;

import net.minecraft.SharedConstants;

public final class Version {

    public static int getCurrentVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    private Version() {}
}
