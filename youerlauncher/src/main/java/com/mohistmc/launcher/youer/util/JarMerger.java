package com.mohistmc.launcher.youer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarMerger {
    private static final Set<String> processedEntries = new HashSet<>();

    public static void mergeJars(File jarFile1, File jarFile2, File outputJar) throws IOException {
        processedEntries.clear();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            addJarContents(jarFile1, jos);
            addJarContents(jarFile2, jos);
        }
    }

    private static void addJarContents(File jarFile, JarOutputStream jos) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            jar.stream().forEach(entry -> {
                try {
                    if (!entry.isDirectory() && !processedEntries.contains(entry.getName())) {
                        processedEntries.add(entry.getName());
                        JarEntry newEntry = new JarEntry(entry.getName());
                        jos.putNextEntry(newEntry);
                        try (InputStream is = jar.getInputStream(entry)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                jos.write(buffer, 0, bytesRead);
                            }
                        }
                        jos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error processing entry: " + entry.getName(), e);
                }
            });
        }
    }
}
