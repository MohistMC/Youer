package com.mohistmc.youer.region;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.slf4j.Logger;

// Linear region format (github.com/xymb-endcrystalme/LinearRegionFileFormatTools), v1 spec.
// Unlike Anvil, an entire 32x32 region is compressed as a single zstd stream, so there is no in-place
// partial update: writes replace an in-memory slot. The background flusher briefly snapshots those
// immutable byte arrays, then serializes and compresses the WHOLE snapshot without holding this
// region's monitor, so reads and later writes do not wait for compression or disk I/O.
public class LinearRegionFile extends RegionFile {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String LINEAR_EXTENSION = ".linear";

    private static final long SIGNATURE = 0xc3ff13183cca9d9aL;
    private static final int SUPPORTED_VERSION = 1;
    private static final int ENTRY_COUNT = 1024; // 32 x 32 chunks per region
    // signature(8) + version(1) + newest_timestamp(8) + compression_level(1) + chunk_count(2) + body_length(4) + reserved(8)
    private static final int OUTER_HEADER_LENGTH = 32;
    private static final int INNER_HEADER_LENGTH = ENTRY_COUNT * 8; // (size:uint32, timestamp:uint32) per slot
    private static final byte[] RESERVED = new byte[8];

    private final RegionStorageInfo info;
    private final Path path;
    private final Path folder;
    private final boolean sync;

    private final byte[][] chunks = new byte[ENTRY_COUNT][];
    private final int[] timestamps = new int[ENTRY_COUNT];
    private long currentGeneration;
    private long persistedGeneration;
    private long completedFlushAttempts;
    private IOException lastFlushFailure;
    private boolean closing;
    private boolean closed;

    public LinearRegionFile(RegionStorageInfo info, Path path, Path folder, boolean sync) throws IOException {
        super(info, path, folder);
        this.info = info;
        this.path = path;
        this.folder = folder;
        this.sync = sync;
        if (Files.isRegularFile(path)) {
            this.load();
        }
    }

    private static int getIndex(ChunkPos pos) {
        return pos.getRegionLocalX() + pos.getRegionLocalZ() * 32;
    }

    private static int getTimestamp() {
        return (int) (Util.getEpochMillis() / 1000L);
    }

    private void load() throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(this.path)))) {
            long signature = in.readLong();
            if (signature != SIGNATURE) {
                LOGGER.error("Linear region file {} has an invalid signature (0x{}); treating it as empty so its chunks will be regenerated", this.path, Long.toHexString(signature));
                return;
            }

            int version = in.readUnsignedByte();
            if (version != SUPPORTED_VERSION) {
                LOGGER.error("Linear region file {} has unsupported version {} (only version {} is supported); treating it as empty so its chunks will be regenerated", this.path, version, SUPPORTED_VERSION);
                return;
            }

            in.readLong(); // newest_timestamp - informational only, recomputed from per-chunk timestamps on write
            in.readUnsignedByte(); // compression_level - informational only, zstd frames are self-describing
            int declaredChunkCount = in.readUnsignedShort();
            int bodyLength = in.readInt();
            in.readFully(new byte[8]); // reserved/hash - not verified for v1 compatibility, per spec

            if (bodyLength < 0) {
                LOGGER.error("Linear region file {} declares a negative body length {}; treating it as empty so its chunks will be regenerated", this.path, bodyLength);
                return;
            }

            byte[] compressedBody = in.readNBytes(bodyLength);
            if (compressedBody.length != bodyLength) {
                LOGGER.error("Linear region file {} is truncated: expected {} compressed body bytes but only read {}; treating it as empty so its chunks will be regenerated", this.path, bodyLength, compressedBody.length);
                return;
            }

            long footerSignature = in.readLong();
            if (footerSignature != SIGNATURE) {
                LOGGER.error("Linear region file {} has an invalid footer signature (0x{}); it is likely truncated or corrupt, treating it as empty so its chunks will be regenerated", this.path, Long.toHexString(footerSignature));
                return;
            }

            byte[] decompressed;
            try (DataInputStream zin = new DataInputStream(new ZstdInputStream(new ByteArrayInputStream(compressedBody)))) {
                decompressed = zin.readAllBytes();
            }

            if (decompressed.length < INNER_HEADER_LENGTH) {
                LOGGER.error("Linear region file {} decompressed body ({} bytes) is smaller than the inner header ({} bytes); treating it as empty so its chunks will be regenerated", this.path, decompressed.length, INNER_HEADER_LENGTH);
                return;
            }

            int[] sizes = new int[ENTRY_COUNT];
            try (DataInputStream headerIn = new DataInputStream(new ByteArrayInputStream(decompressed, 0, INNER_HEADER_LENGTH))) {
                for (int i = 0; i < ENTRY_COUNT; i++) {
                    sizes[i] = headerIn.readInt();
                    this.timestamps[i] = headerIn.readInt();
                }
            }

            int offset = INNER_HEADER_LENGTH;
            int loaded = 0;
            for (int i = 0; i < ENTRY_COUNT; i++) {
                int size = sizes[i];
                if (size == 0) {
                    continue;
                }
                if (size < 0 || offset + size > decompressed.length) {
                    LOGGER.error(
                        "Linear region file {} chunk slot {} declares an out-of-bounds size {} (offset {}, decompressed body length {}); the remaining chunk slot(s) in this file will be dropped and regenerated",
                        this.path, i, size, offset, decompressed.length
                    );
                    break;
                }
                this.chunks[i] = Arrays.copyOfRange(decompressed, offset, offset + size);
                offset += size;
                loaded++;
            }

            if (loaded != declaredChunkCount) {
                LOGGER.debug("Linear region file {} header chunk_count {} does not match {} chunk(s) actually parsed", this.path, declaredChunkCount, loaded);
            }
        } catch (EOFException eofexception) {
            LOGGER.error("Linear region file {} is truncated (unexpected end of file while reading header); treating it as empty so its chunks will be regenerated: {}", this.path, eofexception.toString());
            Arrays.fill(this.chunks, null);
            Arrays.fill(this.timestamps, 0);
        } catch (RuntimeException runtimeexception) {
            // com.github.luben.zstd throws unchecked exceptions on a malformed/corrupt zstd frame
            LOGGER.error("Linear region file {} could not be decoded, treating it as empty so its chunks will be regenerated: {}", this.path, runtimeexception.toString(), runtimeexception);
            Arrays.fill(this.chunks, null);
            Arrays.fill(this.timestamps, 0);
        }
    }

    @Override
    public Path getPath() {
        return this.path;
    }

    @Nullable
    @Override
    public synchronized DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException {
        this.ensureOpen();
        byte[] data = this.chunks[getIndex(pos)];
        return data == null ? null : new DataInputStream(new ByteArrayInputStream(data));
    }

    @Override
    public synchronized DataOutputStream getChunkDataOutputStream(ChunkPos pos) throws IOException {
        this.ensureWritable();
        return new DataOutputStream(new ChunkBuffer(pos));
    }

    @Override
    public synchronized boolean hasChunk(ChunkPos pos) {
        return this.chunks[getIndex(pos)] != null;
    }

    @Override
    public synchronized boolean doesChunkExist(ChunkPos pos) {
        // Unlike Anvil's sector-offset bookkeeping, there is nothing further to sanity-check beyond "did this
        // slot load successfully" once the region file itself has been parsed - equivalent to hasChunk() here.
        return this.hasChunk(pos);
    }

    // Sum of currently-held decompressed chunk bytes, used by RegionFileStorage's memory-budget eviction
    public synchronized long estimatedMemoryUsageBytes() {
        long total = 0L;
        for (byte[] chunk : this.chunks) {
            if (chunk != null) {
                total += chunk.length;
            }
        }
        return total;
    }

    @Override
    public synchronized void clear(ChunkPos pos) throws IOException {
        this.ensureWritable();
        int index = getIndex(pos);
        if (this.chunks[index] != null) {
            this.chunks[index] = null;
            this.timestamps[index] = getTimestamp();
            this.currentGeneration++;
            LinearRegionFileFlusher.scheduleFlush(this);
        }
    }

    @Override
    public void flush() throws IOException {
        final long targetGeneration;
        synchronized (this) {
            this.ensureOpen();
            targetGeneration = this.currentGeneration;
        }
        LinearRegionFileFlusher.flushAndWait(this, targetGeneration);
    }

    @Override
    public void close() throws IOException {
        final long targetGeneration;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closing = true;
            targetGeneration = this.currentGeneration;
        }

        try {
            LinearRegionFileFlusher.flushAndWait(this, targetGeneration);
            synchronized (this) {
                this.closed = true;
            }
            LinearRegionFileFlusher.forget(this);
        } catch (IOException ioexception) {
            synchronized (this) {
                this.closing = false;
            }
            throw ioexception;
        }
    }

    // Overridable seam so tests can supply a fixed level without a running server's GlobalConfiguration
    protected int getCompressionLevel() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().unsupportedSettings.linearCompressionLevel;
    }

    // Overridable seam so tests can supply a fixed thread count without a running server's GlobalConfiguration
    protected int getCompressionThreads() {
        return io.papermc.paper.configuration.GlobalConfiguration.get().unsupportedSettings.linearCompressionThreads;
    }

    synchronized long currentGeneration() {
        return this.currentGeneration;
    }

    synchronized boolean isPersisted(long generation) {
        return this.persistedGeneration >= generation;
    }

    synchronized long completedFlushAttempts() {
        return this.completedFlushAttempts;
    }

    synchronized IOException awaitFlushProgress(long targetGeneration, long observedAttempt) throws InterruptedException {
        while (this.persistedGeneration < targetGeneration && this.completedFlushAttempts == observedAttempt) {
            this.wait();
        }
        return this.completedFlushAttempts == observedAttempt ? null : this.lastFlushFailure;
    }

    synchronized FlushSnapshot createFlushSnapshot() {
        if (this.persistedGeneration >= this.currentGeneration) {
            return null;
        }
        return new FlushSnapshot(this.chunks.clone(), this.timestamps.clone(), this.currentGeneration);
    }

    synchronized boolean completeFlush(FlushSnapshot snapshot, IOException failure) {
        if (failure == null) {
            this.persistedGeneration = Math.max(this.persistedGeneration, snapshot.generation());
        }
        this.lastFlushFailure = failure;
        this.completedFlushAttempts++;
        this.notifyAll();
        return this.persistedGeneration < this.currentGeneration;
    }

    void writeSnapshot(FlushSnapshot snapshot) throws IOException {
        int chunkCount = 0;
        for (byte[] chunk : snapshot.chunks()) {
            if (chunk != null) {
                chunkCount++;
            }
        }

        int level = this.getCompressionLevel();
        int workers = this.getCompressionThreads(); // zstd's own internal multi-threaded compression
        int newestTimestamp = 0;
        for (int timestamp : snapshot.timestamps()) {
            newestTimestamp = Math.max(newestTimestamp, timestamp);
        }

        FileUtil.createDirectoriesSafe(this.folder);
        Path tmp = Files.createTempFile(this.folder, "linear-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.position(OUTER_HEADER_LENGTH);
                OutputStream channelOutput = Channels.newOutputStream(channel);
                try (ZstdOutputStream zout = new ZstdOutputStream(new NonClosingOutputStream(channelOutput), level)) {
                    if (workers > 0) {
                        zout.setWorkers(workers);
                    }
                    DataOutputStream bodyOut = new DataOutputStream(zout);
                    for (int i = 0; i < ENTRY_COUNT; i++) {
                        byte[] chunk = snapshot.chunks()[i];
                        bodyOut.writeInt(chunk == null ? 0 : chunk.length);
                        bodyOut.writeInt(snapshot.timestamps()[i]);
                    }
                    for (byte[] chunk : snapshot.chunks()) {
                        if (chunk != null) {
                            bodyOut.write(chunk);
                        }
                    }
                }

                long compressedLength = channel.position() - OUTER_HEADER_LENGTH;
                if (compressedLength > Integer.MAX_VALUE) {
                    throw new IOException("Linear region compressed body exceeds the v1 format limit: " + compressedLength + " bytes");
                }

                ByteBuffer footer = ByteBuffer.allocate(Long.BYTES).putLong(SIGNATURE);
                footer.flip();
                writeFully(channel, footer);

                ByteBuffer header = ByteBuffer.allocate(OUTER_HEADER_LENGTH);
                header.putLong(SIGNATURE);
                header.put((byte) SUPPORTED_VERSION);
                header.putLong(newestTimestamp);
                header.put((byte) level);
                header.putShort((short) chunkCount);
                header.putInt((int) compressedLength);
                header.put(RESERVED);
                header.flip();
                channel.position(0L);
                writeFully(channel, header);
                if (this.sync) {
                    channel.force(true);
                }
            }

            try {
                Files.move(tmp, this.path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioexception) {
            Files.deleteIfExists(tmp);
            throw ioexception;
        } catch (RuntimeException runtimeexception) {
            Files.deleteIfExists(tmp);
            throw new IOException("Failed to encode linear region file " + this.path, runtimeexception);
        }
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Linear region file is closed: " + this.path);
        }
    }

    private void ensureWritable() throws IOException {
        this.ensureOpen();
        if (this.closing) {
            throw new IOException("Linear region file is closing: " + this.path);
        }
    }

    private class ChunkBuffer extends ByteArrayOutputStream {
        private final ChunkPos pos;

        ChunkBuffer(ChunkPos pos) {
            super(8096);
            this.pos = pos;
        }

        @Override
        public void close() throws IOException {
            synchronized (LinearRegionFile.this) {
                LinearRegionFile.this.ensureWritable();
                int index = getIndex(this.pos);
                LinearRegionFile.this.chunks[index] = this.toByteArray();
                LinearRegionFile.this.timestamps[index] = getTimestamp();
                LinearRegionFile.this.currentGeneration++;
                LinearRegionFileFlusher.scheduleFlush(LinearRegionFile.this);
            }
        }
    }

    static record FlushSnapshot(byte[][] chunks, int[] timestamps, long generation) {}

    private static final class NonClosingOutputStream extends FilterOutputStream {
        private NonClosingOutputStream(OutputStream output) {
            super(output);
        }

        @Override
        public void close() throws IOException {
            this.flush();
        }
    }
}
