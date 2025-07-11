package io.papermc.reflectionrewriter.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

final class DefineClassReflectionProxyImpl implements DefineClassReflectionProxy {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final Map<MethodKey, MethodHandle> handles = new ConcurrentHashMap<>();
    private final Function<byte[], byte[]> classTransformer;

    DefineClassReflectionProxyImpl(final Function<byte[], byte[]> classTransformer) {
        this.classTransformer = classTransformer;
    }

    @Override
    public Class<?> defineClass(final Object loader, final byte[] b, final int off, final int len) throws ClassFormatError {
        try {
            final MethodHandle handle = this.defineClassHandle(
                loader.getClass(),
                MethodType.methodType(Class.class, byte[].class, int.class, int.class)
            );
            if (!(loader instanceof ClassLoader)) {
                return (Class<?>) handle.invokeExact(loader, b, off, len);
            }
            final byte[] bytes = slice(b, off, len);
            final byte[] transformed = this.classTransformer.apply(bytes);
            return (Class<?>) handle.invokeExact(loader, transformed, 0, transformed.length);
        } catch (final Throwable ex) {
            throw sneakyThrow(ex);
        }
    }

    @Override
    public Class<?> defineClass(final Object loader, final String name, final byte[] b, final int off, final int len) throws ClassFormatError {
        try {
            final MethodHandle handle = this.defineClassHandle(
                loader.getClass(),
                MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class)
            );
            if (!(loader instanceof ClassLoader)) {
                return (Class<?>) handle.invokeExact(loader, name, b, off, len);
            }
            final byte[] bytes = slice(b, off, len);
            final byte[] transformed = this.classTransformer.apply(bytes);
            return (Class<?>) handle.invokeExact(loader, name, transformed, 0, transformed.length);
        } catch (final Throwable ex) {
            throw sneakyThrow(ex);
        }
    }

    @Override
    public Class<?> defineClass(final Object loader, final @Nullable String name, final byte[] b, final int off, final int len, final @Nullable ProtectionDomain protectionDomain) throws ClassFormatError {
        try {
            final MethodHandle handle = this.defineClassHandle(
                loader.getClass(),
                MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class)
            );
            if (!(loader instanceof ClassLoader)) {
                return (Class<?>) handle.invokeExact(loader, name, b, off, len, protectionDomain);
            }
            final byte[] bytes = slice(b, off, len);
            final byte[] transformed = this.classTransformer.apply(bytes);
            return (Class<?>) handle.invokeExact(loader, name, transformed, 0, transformed.length, protectionDomain);
        } catch (final Throwable ex) {
            throw sneakyThrow(ex);
        }
    }

    @Override
    public Class<?> defineClass(final Object loader, final String name, final ByteBuffer b, final ProtectionDomain protectionDomain) throws ClassFormatError {
        try {
            final MethodHandle handle = this.defineClassHandle(
                loader.getClass(),
                MethodType.methodType(Class.class, String.class, ByteBuffer.class, ProtectionDomain.class)
            );
            if (!(loader instanceof ClassLoader)) {
                return (Class<?>) handle.invokeExact(loader, name, b, protectionDomain);
            }
            final byte[] bytes = slice(b);
            final byte[] transformed = this.classTransformer.apply(bytes);
            return (Class<?>) handle.invokeExact(loader, name, ByteBuffer.wrap(transformed), protectionDomain);
        } catch (final Throwable ex) {
            throw sneakyThrow(ex);
        }
    }

    @Override
    public Class<?> defineClass(final Object secureLoader, final String name, final byte[] b, final int off, final int len, final CodeSource cs) {
        try {
            final MethodHandle handle = this.defineClassHandle(
                secureLoader.getClass(),
                MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class, CodeSource.class)
            );
            if (!(secureLoader instanceof SecureClassLoader)) {
                return (Class<?>) handle.invokeExact(secureLoader, name, b, off, len, cs);
            }
            final byte[] bytes = slice(b, off, len);
            final byte[] transformed = this.classTransformer.apply(bytes);
            return (Class<?>) handle.invokeExact(secureLoader, name, transformed, 0, transformed.length, cs);
        } catch (final Throwable ex) {
            throw sneakyThrow(ex);
        }
    }

    @Override
    public Class<?> defineClass(final Object secureLoader, final String name, final ByteBuffer b, final CodeSource cs) {
        try {
            final MethodHandle handle = this.defineClassHandle(
                secureLoader.getClass(),
                MethodType.methodType(Class.class, String.class, ByteBuffer.class, CodeSource.class)
            );
            if (!(secureLoader instanceof SecureClassLoader)) {
                return (Class<?>) handle.invokeExact(secureLoader, name, b, cs);
            }
            final byte[] bytes = slice(b);
            final byte[] transformed = this.classTransformer.apply(bytes);
            return (Class<?>) handle.invokeExact(secureLoader, name, ByteBuffer.wrap(transformed), cs);
        } catch (final Throwable ex) {
            throw sneakyThrow(ex);
        }
    }

    @Override
    public Class<?> defineClass(final MethodHandles.Lookup lookup, final byte[] bytes) throws IllegalAccessException {
        return lookup.defineClass(this.classTransformer.apply(bytes));
    }

    private MethodHandle defineClassHandle(
        final Class<?> loaderType,
        final MethodType methodType
    ) {
        return this.handles.computeIfAbsent(new MethodKey(loaderType, methodType), methodKey -> {
            try {
                return MethodHandles.privateLookupIn(methodKey.owner(), LOOKUP)
                    .findVirtual(methodKey.owner(), "defineClass", methodKey.methodType())
                    .asType(methodKey.methodType().insertParameterTypes(0, Object.class));
            } catch (final ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to lookup defineClass handle", ex);
            }
        });
    }

    private static byte[] slice(final byte[] bytes, final int start, final int len) {
        final byte[] ret = new byte[len];
        System.arraycopy(bytes, start, ret, 0, len);
        return ret;
    }

    private static byte[] slice(final ByteBuffer b) {
        final int len = b.remaining();
        final byte[] tb = new byte[len];
        if (b.hasArray()) {
            System.arraycopy(b.array(), b.position() + b.arrayOffset(), tb, 0, len);
        } else {
            b.get(tb);
        }
        return tb;
    }

    @SuppressWarnings("unchecked")
    private static <X extends Throwable> X sneakyThrow(final Throwable ex) throws X {
        throw (X) ex;
    }

    // All the methods we use are named defineClass and are distinguished by descriptor, so we don't need the name in the key
    private record MethodKey(Class<?> owner, MethodType methodType) {
    }
}
