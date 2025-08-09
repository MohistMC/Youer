package io.papermc.paper.entity;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.sound.Sound;
import net.minecraft.world.entity.Shearable;
import org.jetbrains.annotations.NotNull;

public interface PaperShearable extends io.papermc.paper.entity.Shearable {

    Shearable getHandle();

    @Override
    default boolean readyToBeSheared() {
        return this.getHandle().readyForShearing();
    }

    @Override
    default void shear(@NotNull Sound.Source source) {
        this.getHandle().shear(PaperAdventure.asVanilla(source));
    }
}
