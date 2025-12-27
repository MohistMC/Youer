package com.mohistmc.launcher.youer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Mgazul
 * @date 2025/12/26 14:53
 */
public class JarModifier {

    public static void removeDirectoryFromJar(String jarPath, String dirToRemove) throws IOException {
        Path tempDir = Files.createTempDirectory("jar-mod-");
        Path originalJar = Paths.get(jarPath);
        Path tempJar = tempDir.resolve("modified.jar");

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(originalJar));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempJar))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // Skip the directory you want to delete and all of its contents
                if (name.startsWith(dirToRemove)) {
                    continue;
                }

                zos.putNextEntry(new ZipEntry(name));
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                zis.closeEntry();
            }
        }

        // Replace the original JAR
        Files.move(tempJar, originalJar, StandardCopyOption.REPLACE_EXISTING);
    }

}
