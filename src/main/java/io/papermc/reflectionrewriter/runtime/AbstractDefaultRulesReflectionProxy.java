package io.papermc.reflectionrewriter.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public abstract class AbstractDefaultRulesReflectionProxy implements DefaultRulesReflectionProxy {
    protected AbstractDefaultRulesReflectionProxy() {
    }

    protected abstract String mapClassName(String name);

    protected abstract String mapDeclaredMethodName(Class<?> clazz, String name, Class<?>... parameterTypes);

    protected abstract String mapMethodName(Class<?> clazz, String name, Class<?>... parameterTypes);

    protected abstract String mapDeclaredFieldName(Class<?> clazz, String name);

    protected abstract String mapFieldName(Class<?> clazz, String name);

    protected final String mapClassOrArrayName(final String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return name;
        }

        // Array type
        if (name.charAt(0) == '[') {
            final int last = name.lastIndexOf('[');

            try {
                // Object array
                if (name.charAt(last + 1) == 'L') {
                    final String cls = name.substring(last + 2, name.length() - 1);
                    return name.substring(0, last + 2) + this.mapClassName(cls) + ';';
                }
            } catch (final IndexOutOfBoundsException ex) {
                // Pass through on invalid names
                return name;
            }

            // Primitive array
            return name;
        }

        return this.mapClassName(name);
    }

    private static ClassLoader callerClassLoader() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(stream -> stream.skip(3).findFirst().map(frame -> frame.getDeclaringClass().getClassLoader()).orElseThrow());
    }

    // Begin standard reflection
    @Override
    public Class<?> forName(final String name) throws ClassNotFoundException {
        return Class.forName(this.mapClassOrArrayName(name), true, callerClassLoader());
    }

    @Override
    public Class<?> forName(final String name, final boolean initialize, final ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(this.mapClassOrArrayName(name), initialize, loader);
    }

    @Override
    public Class<?> forName(final Module module, final String name) {
        // Uses the module's class loader unless a SecurityManager is used
        // todo pass calling classloader when SecurityManager is used?
        return Class.forName(module, this.mapClassOrArrayName(name));
    }

    @Override
    public Field getField(final Class<?> clazz, final String name) throws NoSuchFieldException, SecurityException {
        return clazz.getField(this.mapFieldName(clazz, name));
    }

    @Override
    public Field getDeclaredField(final Class<?> clazz, final String name) throws NoSuchFieldException, SecurityException {
        return clazz.getDeclaredField(this.mapDeclaredFieldName(clazz, name));
    }

    @Override
    public Method getDeclaredMethod(final Class<?> clazz, final String name, final Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        return clazz.getDeclaredMethod(this.mapDeclaredMethodName(clazz, name, parameterTypes), parameterTypes);
    }

    @Override
    public Method getMethod(final Class<?> clazz, final String name, final Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        return clazz.getMethod(this.mapMethodName(clazz, name, parameterTypes), parameterTypes);
    }
    // End standard reflection

    // Begin MethodHandles
    @Override
    public MethodHandle findStatic(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final MethodType type) throws NoSuchMethodException, IllegalAccessException {
        return lookup.findStatic(refc, this.mapMethodName(refc, name, type.parameterArray()), type);
    }

    @Override
    public MethodHandle findVirtual(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final MethodType type) throws NoSuchMethodException, IllegalAccessException {
        return lookup.findVirtual(refc, this.mapMethodName(refc, name, type.parameterArray()), type);
    }

    @Override
    public Class<?> findClass(final MethodHandles.Lookup lookup, final String targetName) throws ClassNotFoundException, IllegalAccessException {
        return lookup.findClass(this.mapClassOrArrayName(targetName));
    }

    @Override
    public MethodHandle findSpecial(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final MethodType type, final Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
        return lookup.findSpecial(refc, this.mapMethodName(refc, name, type.parameterArray()), type, specialCaller);
    }

    @Override
    public MethodHandle findGetter(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        return lookup.findGetter(refc, this.mapFieldName(refc, name), type);
    }

    @Override
    public MethodHandle findSetter(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        return lookup.findSetter(refc, this.mapFieldName(refc, name), type);
    }

    @Override
    public MethodHandle findStaticGetter(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        return lookup.findStaticGetter(refc, this.mapFieldName(refc, name), type);
    }

    @Override
    public MethodHandle findStaticSetter(final MethodHandles.Lookup lookup, final Class<?> refc, final String name, final Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        return lookup.findStaticSetter(refc, this.mapFieldName(refc, name), type);
    }

    @Override
    public MethodHandle bind(final MethodHandles.Lookup lookup, final Object receiver, final String name, final MethodType type) throws NoSuchMethodException, IllegalAccessException {
        return lookup.bind(receiver, this.mapMethodName(receiver.getClass(), name, type.parameterArray()), type);
    }

    @Override
    public VarHandle findVarHandle(final MethodHandles.Lookup lookup, final Class<?> recv, final String name, final Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        return lookup.findVarHandle(recv, this.mapFieldName(recv, name), type);
    }

    @Override
    public VarHandle findStaticVarHandle(final MethodHandles.Lookup lookup, final Class<?> decl, final String name, final Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        return lookup.findStaticVarHandle(decl, this.mapFieldName(decl, name), type);
    }
    // End MethodHandles

    // Begin LambdaMetafactory
    @Override
    public CallSite metafactory(final MethodHandles.Lookup caller, final String interfaceMethodName, final MethodType factoryType, final MethodType interfaceMethodType, final MethodHandle implementation, final MethodType dynamicMethodType) throws LambdaConversionException {
        return LambdaMetafactory.metafactory(caller, this.mapMethodName(factoryType.returnType(), interfaceMethodName, dynamicMethodType.parameterArray()), factoryType, interfaceMethodType, implementation, dynamicMethodType);
    }

    @Override
    public CallSite altMetafactory(final MethodHandles.Lookup caller, final String interfaceMethodName, final MethodType factoryType, final Object... args) throws LambdaConversionException {
        if (args.length < 3 || !(args[2] instanceof MethodType dynamicMethodType)) {
            throw new IllegalArgumentException("illegal or missing argument");
        }
        return LambdaMetafactory.altMetafactory(caller, this.mapMethodName(factoryType.returnType(), interfaceMethodName, dynamicMethodType.parameterArray()), factoryType, args);
    }
    // End LambdaMetafactory

    // Begin ConstantBootstraps
    @Override
    public Object getStaticFinal(final MethodHandles.Lookup lookup, final String name, final Class<?> type, final Class<?> declaringClass) {
        return ConstantBootstraps.getStaticFinal(lookup, this.mapFieldName(declaringClass, name), type, declaringClass);
    }

    @Override
    public Object getStaticFinal(final MethodHandles.Lookup lookup, final String name, final Class<?> type) {
        return ConstantBootstraps.getStaticFinal(lookup, this.mapFieldName(type, name), type);
    }

    @Override
    public VarHandle fieldVarHandle(final MethodHandles.Lookup lookup, final String name, final Class<VarHandle> type, final Class<?> declaringClass, final Class<?> fieldType) {
        return ConstantBootstraps.fieldVarHandle(lookup, this.mapFieldName(declaringClass, name), type, declaringClass, fieldType);
    }

    @Override
    public VarHandle staticFieldVarHandle(final MethodHandles.Lookup lookup, final String name, final Class<VarHandle> type, final Class<?> declaringClass, final Class<?> fieldType) {
        return ConstantBootstraps.staticFieldVarHandle(lookup, this.mapFieldName(declaringClass, name), type, declaringClass, fieldType);
    }
    // End ConstantBootstraps

    // Begin MethodType
    @Override
    public MethodType fromMethodDescriptorString(final String descriptor, final ClassLoader loader) throws IllegalArgumentException, TypeNotPresentException {
        if (descriptor.indexOf('L') == -1) {
            // Only primitives in descriptor, nothing to remap
            return MethodType.fromMethodDescriptorString(descriptor, loader);
        }

        final StringBuilder desc = new StringBuilder(descriptor.length());

        // Search and replace L(class/Name); -> L(remapped/Name);
        int i = 0;
        final int length = descriptor.length();
        while (i < length) {
            final char c = descriptor.charAt(i);
            i++;
            desc.append(c);
            if (c == 'L') {
                final int endIndex = descriptor.indexOf(';', i);
                if (endIndex == -1) {
                    throw new IllegalArgumentException("'" + descriptor + "' is not a valid method descriptor.");
                }
                final String unmappedSlash = descriptor.substring(i, endIndex);
                final String unmappedDot = unmappedSlash.replace('/', '.'); // replace slash with dot for class mappings
                final String mappedDot = this.mapClassName(unmappedDot);
                final String result = mappedDot.equals(unmappedDot)
                    ? unmappedSlash // Avoid double replace if mapping didn't make a change
                    : mappedDot.replace('.', '/'); // back to slash for the descriptor string
                i = endIndex;
                desc.append(result);
            }
        }
        return MethodType.fromMethodDescriptorString(desc.toString(), loader);
    }
    // End MethodType
}
