package com.mohistmc.youer.feature;

import com.github.luben.zstd.ZstdOutputStream;
import com.mohistmc.tools.NamedThreadFactory;
import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.util.I18n;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class WorldBackup {

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("WorldBackup"));

    private static final int ZSTD_LEVEL = 15;

    public static void start() {
        if (YouerConfig.backup_world_enable) {
            SCHEDULER.scheduleAtFixedRate(() -> {
                if (MinecraftServer.getServer().hasStopped()) return;
                backup();
            }, 1000L * YouerConfig.backup_world_interval, 1000L * YouerConfig.backup_world_interval, TimeUnit.MILLISECONDS);
            Youer.LOGGER.info(I18n.as("worldbackupcmd.notice.backupScheduled", YouerConfig.backup_world_interval));
        }
    }

    public static void stop() {
        SCHEDULER.shutdown();
    }

    public static void backup(CommandSender sender) {
        sender.sendMessage(I18n.as("worldbackupcmd.notice.creatingWorldBackup"));
        new Thread(() -> {
            try {
                doBackup();
                sender.sendMessage(I18n.as("worldbackupcmd.notice.worldComplete"));
            } catch (Exception e) {
                Youer.LOGGER.error("Failed to backup world.", e);
            }
        }).start();
    }

    public static void backup() {
        try {
            doBackup();
            Youer.LOGGER.info(I18n.as("worldbackupcmd.notice.worldCompleteLog"));
        } catch (Exception e) {
            Youer.LOGGER.error("Failed to backup world.", e);
        }
    }

    private static void doBackup() throws IOException {
        Path worldPath = Paths.get(MinecraftServer.getServer().getLevel(Level.OVERWORLD).name);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        boolean useZstd = YouerConfig.backup_world_use_zstd;
        String ext = useZstd ? ".tar.zst" : ".zip";
        File backupDir = new File("./YouerBackups");
        backupDir.mkdirs();

        File zip = new File(backupDir, "world-" + timestamp + ext);
        backupDirectory(worldPath, zip, useZstd);
        cleanupOldBackups(backupDir, "world-");

        for (World world : Bukkit.getWorlds()) {
            if (world.isBukkit()) {
                // Bukkit世界的存储目录独立于主世界，需单独备份
                Path bukkitWorldPath = world.getWorldFolder().toPath();
                String worldName = world.getName();
                File bukkitZip = new File(backupDir, worldName + "-" + timestamp + ext);
                backupDirectory(bukkitWorldPath, bukkitZip, useZstd);
                cleanupOldBackups(backupDir, worldName + "-");
            }
        }
    }

    private static void backupDirectory(Path sourceDir, File targetFile, boolean useZstd) throws IOException {
        if (useZstd) {
            try (OutputStream fos = Files.newOutputStream(targetFile.toPath());
                 ZstdOutputStream zos = new ZstdOutputStream(fos, ZSTD_LEVEL)) {
                writeTarArchive(sourceDir, zos);
            }
        } else {
            try (OutputStream fos = Files.newOutputStream(targetFile.toPath());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                Files.walk(sourceDir)
                        .filter(p -> !p.equals(sourceDir))
                        .filter(p -> !p.getFileName().toString().equals("session.lock"))
                        .forEach(p -> addToZip(sourceDir, p, zos));
            }
        }
    }

    private static void addToZip(Path baseDir, Path file, ZipOutputStream zos) {
        try {
            String entryName = baseDir.relativize(file).toString().replace("\\", "/");
            if (Files.isDirectory(file)) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(file.toFile())) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            Youer.LOGGER.warn(I18n.as("worldbackupcmd.notice.skippedLockedFile", file.getFileName()), e);
        }
    }

    private static void writeTarArchive(Path baseDir, OutputStream os) throws IOException {
        try (var stream = Files.walk(baseDir)
                .filter(p -> !p.equals(baseDir))
                .filter(p -> !p.getFileName().toString().equals("session.lock"))
                .sorted()) {
            var list = stream.toList();
            for (Path file : list) {
                writeTarEntry(baseDir, file, os);
            }
            // End of archive: two 512-byte zero blocks
            os.write(new byte[1024]);
            os.flush();
        }
    }

    private static void writeTarEntry(Path baseDir, Path file, OutputStream os) throws IOException {
        try {
            String entryName = baseDir.relativize(file).toString().replace("\\", "/");
            boolean isDir = Files.isDirectory(file);
            long size = isDir ? 0 : Files.size(file);

            byte[] header = new byte[512];

            // File name (100 bytes)
            byte[] nameBytes = entryName.getBytes(StandardCharsets.UTF_8);
            if (nameBytes.length > 100) {
                Youer.LOGGER.warn("Path too long for TAR, skipping: {}", entryName);
                return;
            }
            System.arraycopy(nameBytes, 0, header, 0, nameBytes.length);

            // Mode (8 bytes)
            String mode = isDir ? "040755" : "0100644";
            System.arraycopy(mode.getBytes(StandardCharsets.US_ASCII), 0, header, 100, mode.length());

            // Size (12 bytes) - octal with leading zeros, trailing space
            String sizeStr = String.format("%011o", size);
            System.arraycopy(sizeStr.getBytes(StandardCharsets.US_ASCII), 0, header, 124, 11);
            header[135] = ' ';

            // MTime (12 bytes)
            String mtimeStr = String.format("%011o", System.currentTimeMillis() / 1000);
            System.arraycopy(mtimeStr.getBytes(StandardCharsets.US_ASCII), 0, header, 136, 11);
            header[147] = ' ';

            // Type flag: '5' = directory, '0' = regular file
            header[156] = isDir ? (byte) '5' : (byte) '0';

            // USTAR magic + version
            System.arraycopy("ustar\0".getBytes(StandardCharsets.US_ASCII), 0, header, 257, 6);
            System.arraycopy("00".getBytes(StandardCharsets.US_ASCII), 0, header, 263, 2);

            // Checksum: sum all bytes with checksum field treated as spaces
            Arrays.fill(header, 148, 156, (byte) ' ');
            int sum = 0;
            for (byte b : header) {
                sum += b & 0xFF;
            }
            String chksumStr = String.format("%06o", sum);
            System.arraycopy(chksumStr.getBytes(StandardCharsets.US_ASCII), 0, header, 148, 6);
            header[154] = ' ';
            header[155] = 0;

            os.write(header);

            // Write file content
            if (!isDir) {
                try (InputStream is = Files.newInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
                // Pad to 512-byte boundary
                int padding = (int) ((512 - (size % 512)) % 512);
                if (padding > 0) {
                    os.write(new byte[padding]);
                }
            }
        } catch (IOException e) {
            Youer.LOGGER.warn(I18n.as("worldbackupcmd.notice.skippedLockedFile", file.getFileName()), e);
        }
    }

    private static void cleanupOldBackups(File backupDir, String prefix) {
        int max = YouerConfig.backup_world_max_backups;
        if (max <= 0) return;

        File[] files = backupDir.listFiles((e, name) -> name.startsWith(prefix)
                && (name.endsWith(".zip") || name.endsWith(".tar.zst") || name.endsWith(".zst")));
        if (files == null || files.length <= max) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int toDelete = files.length - max;
        for (int i = 0; i < toDelete; i++) {
            if (files[i].delete()) {
                Youer.LOGGER.info(I18n.as("worldbackupcmd.notice.deletedOldBackup", files[i].getName()));
            }
        }
    }
}
