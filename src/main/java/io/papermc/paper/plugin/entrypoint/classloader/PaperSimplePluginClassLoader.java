package io.papermc.paper.plugin.entrypoint.classloader;

import com.google.common.io.ByteStreams;
import com.mohistmc.youer.asm.SwitchTableFixer;
import com.mohistmc.youer.bukkit.remapping.ClassLoaderRemapper;
import com.mohistmc.youer.bukkit.remapping.Remapper;
import com.mohistmc.youer.bukkit.remapping.RemappingClassLoader;
import io.izzel.tools.product.Product2;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.util.NamespaceChecker;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a simple classloader used for paper plugin bootstrappers.
 */
@ApiStatus.Internal
public class PaperSimplePluginClassLoader extends URLClassLoader implements RemappingClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    protected final PluginMeta configuration;
    protected final Path source;
    protected final Manifest jarManifest;
    protected final URL jarUrl;
    protected final JarFile jar;

    public PaperSimplePluginClassLoader(Path source, JarFile file, PluginMeta configuration, ClassLoader parentLoader) throws IOException {
        super(source.getFileName().toString(), new URL[]{source.toUri().toURL()}, parentLoader);

        this.source = source;
        this.jarManifest = file.getManifest();
        this.jarUrl = source.toUri().toURL();
        this.configuration = configuration;
        this.jar = file;
    }

    private ClassLoaderRemapper remapper;

    @Override
    public ClassLoaderRemapper getRemapper() {
        if (remapper == null) {
            remapper = Remapper.createClassLoaderRemapper(this);
        }
        return remapper;
    }

    @Override
    public URL getResource(String name) {
        return this.findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return this.findResources(name);
    }

    // Bytecode modification supported loader
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        NamespaceChecker.validateNameSpaceForClassloading(name);

        final Class<?> result;
        final String path = name.replace('.', '/').concat(".class");
        final URL url = this.findResource(path);
        if (url != null) {
            URLConnection connection;
            Callable<byte[]> byteSource;
            Manifest manifest;
            try {
                connection = url.openConnection();
                connection.connect();
                if (connection instanceof JarURLConnection && ((JarURLConnection) connection).getManifest() != null) {
                    manifest = ((JarURLConnection) connection).getManifest();
                } else {
                    manifest = null;
                }
                byteSource = () -> {
                    try (InputStream is = connection.getInputStream()) {
                        byte[] classBytes = ByteStreams.toByteArray(is);
                        classBytes = ClassloaderBytecodeModifier.bytecodeModifier().modify(this.configuration, classBytes);
                        classBytes = SwitchTableFixer.INSTANCE.apply(classBytes);
                        return classBytes;
                    }
                };
                result = this.defineClass(name, byteSource, connection, manifest, url);
            } catch (final Exception e) {
                throw new ClassNotFoundException(name, e);
            }
        } else {
            result = null;
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    private Class<?> defineClass(String name, Callable<byte[]> byteSource, URLConnection connection, Manifest manifest, URL url) throws Exception {
        Product2<byte[], CodeSource> classBytes = this.getRemapper().remapClass(name, byteSource, connection);
        int i = name.lastIndexOf('.');
        if (i != -1) {
            String pkgname = name.substring(0, i);
            final URL jarUrl = URI.create(jarName(url)).toURL();
            if (this.getDefinedPackage(pkgname) == null) {
                try {
                    if (manifest != null) {
                        this.definePackage(pkgname, manifest, jarUrl);
                    } else {
                        this.definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                } catch (IllegalArgumentException iae) {
                    // parallel-capable class loaders: re-verify in case of a
                    // race condition
                    if (this.getDefinedPackage(pkgname) == null) {
                        // Should never happen
                        throw new AssertionError("Cannot find package " +
                                pkgname);
                    }
                }
            }
        }
        return this.defineClass(name, classBytes._1, 0, classBytes._1.length, classBytes._2);
    }

    private static String jarName(final URL sourceUrl) {
        final int exclamationIdx = sourceUrl.getPath().lastIndexOf('!');
        if (exclamationIdx != -1) {
            return sourceUrl.getPath().substring(0, exclamationIdx);
        }
        throw new IllegalArgumentException("Could not find jar for URL " + sourceUrl);
    }

    @Override
    public String toString() {
        return "PaperSimplePluginClassLoader{" +
            "configuration=" + this.configuration +
            ", source=" + this.source +
            ", jarManifest=" + this.jarManifest +
            ", jarUrl=" + this.jarUrl +
            ", jar=" + this.jar +
            '}';
    }
}
