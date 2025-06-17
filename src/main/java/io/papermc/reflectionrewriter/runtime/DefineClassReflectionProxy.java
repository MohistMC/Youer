package io.papermc.reflectionrewriter.runtime;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.function.Function;

public interface DefineClassReflectionProxy {
    // Note:
    //  - The loader parameters are of type Object to account for assumeClassLoader potentially
    //    being true on the DefineClassRule. The built-in implementation will check the type
    //    and pass through to the original method if it's not correct (ClassLoader/SecureClassLoader
    //    or extending class).
    //  - This is why the built-in implementation implements each method separately instead of having
    //    one implemented and having the others call it, to more easily implement the aforementioned
    //    pass through behavior. Furthermore, default methods are not used in this interface to encourage
    //    the implementor to decide whether they will handle assumeClassLoader being true. (again,
    //    the built-in implementation handles both cases)

    // ClassLoader start
    Class<?> defineClass(Object loader, byte[] b, int off, int len) throws ClassFormatError;

    Class<?> defineClass(Object loader, String name, byte[] b, int off, int len) throws ClassFormatError;

    Class<?> defineClass(Object loader, String name, byte[] b, int off, int len, ProtectionDomain protectionDomain) throws ClassFormatError;

    Class<?> defineClass(Object loader, String name, ByteBuffer b, ProtectionDomain protectionDomain) throws ClassFormatError;
    // ClassLoader end

    // SecureClassLoader start
    Class<?> defineClass(Object secureLoader, String name, byte[] b, int off, int len, CodeSource cs);

    Class<?> defineClass(Object secureLoader, String name, ByteBuffer b, CodeSource cs);
    // SecureClassLoader end

    // MethodHandles.Lookup start
    Class<?> defineClass(MethodHandles.Lookup lookup, byte[] bytes) throws IllegalAccessException;
    // MethodHandles.Lookup end

    static DefineClassReflectionProxy create(final Function<byte[], byte[]> classTransformer) {
        return new DefineClassReflectionProxyImpl(classTransformer);
    }
}
