package com.mohistmc.youer.simd;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SIMDDetection {

    public static boolean isEnabled = false;

    @ApiStatus.Internal
    public static boolean canEnable() {
        try {
            return SIMDChecker.canEnable();
        } catch (NoClassDefFoundError | Exception ignored) {
            return false;
        }
    }
}
