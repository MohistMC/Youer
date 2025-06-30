package net.neoforged.neoforge.mixins;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

import static net.minecraft.world.level.Level.fastClip;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor, AutoCloseable{

    /**
     * @reason Route to optimized call
     * @author Spottedleaf
     */
    @Override
    public BlockHitResult clip(final ClipContext clipContext) {
        // can only do this in this class, as not everything that implements BlockGetter can retrieve chunks
        return fastClip(clipContext.getFrom(), clipContext.getTo(), (Level)(Object)this, clipContext);
    }
}
