package com.mohistmc.youer.simd;

import com.mohistmc.youer.Youer;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import org.jetbrains.annotations.ApiStatus;

/**
 * Basically, java is annoying and we have to push this out to its own class.
 */
@ApiStatus.Internal
public class SIMDChecker {

    @ApiStatus.Internal
    public static boolean canEnable() {
        try {
            VectorSpecies<Integer> ISPEC = IntVector.SPECIES_PREFERRED;
            VectorSpecies<Float> FSPEC = FloatVector.SPECIES_PREFERRED;

            Youer.LOGGER.info("Max SIMD vector size on this system is {} bits (int)", ISPEC.vectorBitSize());
            Youer.LOGGER.info("Max SIMD vector size on this system is {} bits (float)", FSPEC.vectorBitSize());

            if (ISPEC.elementSize() < 2 || FSPEC.elementSize() < 2) {
                Youer.LOGGER.warn("SIMD is not properly supported on this system!");
                return false;
            }

            return true;
        } catch (NoClassDefFoundError | Exception ignored) {} // Basically, we don't do anything. This lets us detect if it's not functional and disable it.
        return false;
    }

}
