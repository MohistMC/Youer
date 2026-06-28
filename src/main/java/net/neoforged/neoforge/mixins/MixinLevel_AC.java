package net.neoforged.neoforge.mixins;

import alternate.current.wire.WireHandler;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class MixinLevel_AC implements IWireHandler {

    @Override
    public WireHandler youer$getWireHandler() {
        return null;
    }
}
