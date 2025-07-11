package io.papermc.paper.plugin.entrypoint.classloader;

import com.google.common.io.ByteStreams;
import com.mohistmc.youer.asm.SwitchTableFixer;
import com.mohistmc.youer.bukkit.remapping.ClassLoaderRemapper;
import com.mohistmc.youer.bukkit.remapping.Remapper;
import com.mohistmc.youer.bukkit.remapping.RemappingClassLoader;
import io.izzel.tools.product.Product2;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static java.util.Objects.requireNonNullElse;

public final class BytecodeModifyingURLClassLoader extends URLClassLoader implements RemappingClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private ClassLoaderRemapper remapper;

    @Override
    public ClassLoaderRemapper getRemapper() {
        if (remapper == null) {
            remapper = Remapper.createClassLoaderRemapper(this);
        }
        return remapper;
    }

    private static final Object MISSING_MANIFEST = new Object();

    private final Function<byte[], byte[]> modifier;
    private final Map<String, Object> manifests = new ConcurrentHashMap<>();

    public BytecodeModifyingURLClassLoader(
        final URL[] urls,
        final ClassLoader parent,
        final Function<byte[], byte[]> modifier
    ) {
        super(urls, parent);
        this.modifier = modifier;
    }

    public BytecodeModifyingURLClassLoader(
        final URL[] urls,
        final ClassLoader parent
    ) {
        this(urls, parent, bytes -> {
            final ClassReader classReader = new ClassReader(bytes);
            final ClassWriter classWriter = new ClassWriter(classReader, 0);
            classReader.accept(classWriter, 0);
            return classWriter.toByteArray();
        });
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
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
            if (this.getAndVerifyPackage(pkgname, manifest, jarUrl) == null) {
                try {
                    if (manifest != null) {
                        this.definePackage(pkgname, manifest, jarUrl);
                    } else {
                        this.definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                } catch (IllegalArgumentException iae) {
                    // parallel-capable class loaders: re-verify in case of a
                    // race condition
                    if (this.getAndVerifyPackage(pkgname, manifest, jarUrl) == null) {
                        // Should never happen
                        throw new AssertionError("Cannot find package " +
                            pkgname);
                    }
                }
            }
        }
        return this.defineClass(name, classBytes._1, 0, classBytes._1.length, classBytes._2);
    }

    private Package getAndVerifyPackage(
        String pkgname,
        Manifest man, URL url
    ) {
        Package pkg = getDefinedPackage(pkgname);
        if (pkg != null) {
            // Package found, so check package sealing.
            if (pkg.isSealed()) {
                // Verify that code source URL is the same.
                if (!pkg.isSealed(url)) {
                    throw new SecurityException(
                        "sealing violation: package " + pkgname + " is sealed");
                }
            } else {
                // Make sure we are not attempting to seal the package
                // at this code source URL.
                if ((man != null) && this.isSealed(pkgname, man)) {
                    throw new SecurityException(
                        "sealing violation: can't seal package " + pkgname +
                            ": already loaded");
                }
            }
        }
        return pkg;
    }

    private boolean isSealed(String name, Manifest man) {
        Attributes attr = man.getAttributes(name.replace('.', '/').concat("/"));
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Attributes.Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Attributes.Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    private @Nullable Manifest manifestFor(final URL url) throws IOException {
        Manifest man = null;
        if (url.getProtocol().equals("jar")) {
            try {
                final Object computedManifest = this.manifests.computeIfAbsent(jarName(url), $ -> {
                    try {
                        final Manifest m = ((JarURLConnection) url.openConnection()).getManifest();
                        return requireNonNullElse(m, MISSING_MANIFEST);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                if (computedManifest instanceof Manifest found) {
                    man = found;
                }
            } catch (final UncheckedIOException e) {
                throw e.getCause();
            } catch (final IllegalArgumentException e) {
                throw new IOException(e);
            }
        }
        return man;
    }

    private static String jarName(final URL sourceUrl) {
        final int exclamationIdx = sourceUrl.getPath().lastIndexOf('!');
        if (exclamationIdx != -1) {
            return sourceUrl.getPath().substring(0, exclamationIdx);
        }
        throw new IllegalArgumentException("Could not find jar for URL " + sourceUrl);
    }
}
