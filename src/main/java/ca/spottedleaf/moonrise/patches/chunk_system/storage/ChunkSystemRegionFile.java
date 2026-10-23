package ca.spottedleaf.moonrise.patches.chunk_system.storage;

import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import java.io.IOException;

public interface ChunkSystemRegionFile {

    public MoonriseRegionFileIO.RegionDataController.WriteData moonrise$startWrite(final CompoundTag data, final ChunkPos pos) throws IOException;

    public int moonrise$getRecalculateCount();

    public boolean moonrise$recalculateHeader() throws IOException;

    public @org.jspecify.annotations.Nullable Boolean moonrise$tryRecalcHeader(final int expectedRecalcCount) throws IOException;

}
