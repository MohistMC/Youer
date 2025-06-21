package com.mohistmc.launcher.youer.libraries;

import com.mohistmc.launcher.youer.Main;
import com.mohistmc.tools.SHA256;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.SneakyThrows;
import lombok.ToString;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

@ToString
public class LibrariesDownloadQueue {

    @ToString.Exclude
    public final Set<Libraries> allLibraries = new HashSet<>();
    @ToString.Exclude
    private final Set<Libraries> fail = new HashSet<>();
    @ToString.Exclude
    public Set<Libraries> need_download = new LinkedHashSet<>();

    public String parentDirectory = "libraries";
    public String systemProperty = null;
    public boolean debug = false;


    public static LibrariesDownloadQueue create() {
        return new LibrariesDownloadQueue();
    }

    private static boolean isTargetFile(JarEntry entry) {
        String name = entry.getName().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".txt");
    }

    /**
     * Set the file download directory
     *
     * @param parentDirectory The path to which the file is downloaded
     * @return Returns the current real column
     */
    public LibrariesDownloadQueue parentDirectory(String parentDirectory) {
        this.parentDirectory = parentDirectory;
        return this;
    }

    /**
     * Construct the final column
     *
     * @return Construct the final column
     */
    @SneakyThrows
    public LibrariesDownloadQueue build() {
        scanFromJar();
        return this;
    }

    /**
     * Download in the form of a progress bar
     */
    public void progressBar() {
        if (needDownload()) {
            ProgressBarBuilder builder = new ProgressBarBuilder()
                    .setTaskName("")
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUpdateIntervalMillis(100)
                    .setInitialMax(need_download.size());
            try (ProgressBar pb = builder.build()) {
                for (Libraries lib : need_download) {
                    File file = new File(parentDirectory, lib.path);
                    file.getParentFile().mkdirs();
                    String url = "META-INF/" + file.getPath();
                    if (copyFileFromJar(file, url.replaceAll("\\\\", "/"), lib)) {
                        debug("copyFileFromJar: OK");
                        fail.remove(lib);
                    } else {
                        debug("copyFileFromJar: No " + url);
                        fail.add(lib);
                    }
                    pb.step();
                }
            }
        }
        if (!fail.isEmpty()) {
            progressBar();
        }
    }

    protected boolean copyFileFromJar(File file, String pathInJar, Libraries lib) {
        InputStream is = Main.class.getClassLoader().getResourceAsStream(pathInJar);

        if (!file.exists() || !SHA256.as(file).equals(lib.getSha256()) || file.length() <= 1) {
            file.getParentFile().mkdirs();
            if (is != null) {
                try {
                    file.createNewFile();
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException ignored) {
                }
            } else {
                System.out.println("[Youer] The file " + file.getPath() + " doesn't exists in the Youer jar !");
                return false;
            }
        }
        return true;
    }

    public boolean needDownload() {
        for (Libraries libraries : allLibraries) {
            File lib = new File(parentDirectory, libraries.path);
            if (lib.exists() && Objects.equals(SHA256.as(lib), libraries.getSha256())) {
                continue;
            }
            need_download.add(libraries);
        }
        return !need_download.isEmpty();
    }

    public void scanFromJar() throws IOException {
        Enumeration<URL> resources = LibrariesDownloadQueue.class.getClassLoader().getResources("META-INF/libraries");
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if ("jar".equals(url.getProtocol())) {
                JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                JarFile jarFile = jarConnection.getJarFile();
                String entryPrefix = jarConnection.getEntryName();

                jarFile.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().startsWith(entryPrefix))
                        .filter(LibrariesDownloadQueue::isTargetFile)
                        .forEach(entry -> {
                            String line = entry.getName().substring(entryPrefix.length());
                            InputStream is = Main.class.getClassLoader().getResourceAsStream(entry.getName());
                            Libraries libraries = new Libraries(line, SHA256.as(is), entry.getSize());
                            allLibraries.add(libraries);
                            debug("Find the resource: " + libraries);
                        });
            }
        }
    }

    public void debug(String log) {
        if (debug) System.out.println(log + "\n");
    }
}