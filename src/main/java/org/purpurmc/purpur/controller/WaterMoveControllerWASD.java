package org.purpurmc.purpur.controller;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class WaterMoveControllerWASD extends MoveControllerWASD {
    private final double speedModifier;

    public WaterMoveControllerWASD(Mob entity) {
        this(entity, 1.0D);
    }

    public WaterMoveControllerWASD(Mob entity, double speedModifier) {
        super(entity);
        this.speedModifier = speedModifier;
    }

    @Override
    public void purpurTick(Player rider) {
        float forward = rider.getForwardMot();
        float strafe = rider.getStrafeMot() * 0.5F; // strafe slower by default
        float vertical = -(rider.xRotO / 90);

        if (forward == 0.0F) {
            // strafe slower if not moving forward
            strafe *= 0.5F;
            // do not move vertically if not moving forward
            vertical = 0.0F;
        } else if (forward < 0.0F) {
            // water animals can't swim backwards
            forward = 0.0F;
            vertical = 0.0F;
        }

        if (rider.jumping && spacebarEvent(entity)) {
            entity.onSpacebar();
        }

        setSpeedModifier(entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * speedModifier);
        entity.setSpeed((float) getSpeedModifier() * 0.1F);

        entity.setForwardMot(forward * (float) speedModifier);
        entity.setStrafeMot(strafe * (float) speedModifier);
        entity.setVerticalMot(vertical * (float) speedModifier);

        setForward(entity.getForwardMot());
        setStrafe(entity.getStrafeMot());
    }
}
