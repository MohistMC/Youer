package com.mojang.math;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@FunctionalInterface
public interface Axis {
    Axis XN = angle -> new Quaternionf().rotationX(-angle);
    Axis XP = angle -> new Quaternionf().rotationX(angle);
    Axis YN = angle -> new Quaternionf().rotationY(-angle);
    Axis YP = angle -> new Quaternionf().rotationY(angle);
    Axis ZN = angle -> new Quaternionf().rotationZ(-angle);
    Axis ZP = angle -> new Quaternionf().rotationZ(angle);

    static Axis of(final Vector3f vector) {
        return angle -> new Quaternionf().rotationAxis(angle, vector);
    }

    Quaternionf rotation(float angle);

    default Quaternionf rotationDegrees(final float angle) {
        return this.rotation(angle * Mth.DEG_TO_RAD);
    }
}
