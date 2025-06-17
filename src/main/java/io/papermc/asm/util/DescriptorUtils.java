package io.papermc.asm.util;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Type;

public final class DescriptorUtils {

    public static ClassDesc fromOwner(final String owner) {
        return ClassDesc.ofDescriptor(Type.getObjectType(owner).getDescriptor());
    }

    public static String toOwner(final ClassDesc desc) {
        return Type.getType(desc.descriptorString()).getInternalName();
    }

    public static ClassDesc desc(final Class<?> clazz) {
        return clazz.describeConstable().orElseThrow();
    }

    public static MethodTypeDesc desc(final Executable executable) {
        final org.objectweb.asm.commons.Method asmMethod;
        if (executable instanceof final Method method) {
            asmMethod = org.objectweb.asm.commons.Method.getMethod(method);
        } else if (executable instanceof final Constructor<?> constructor) {
            asmMethod = org.objectweb.asm.commons.Method.getMethod(constructor);
        } else {
            throw new IllegalArgumentException(executable + " isn't a constructor or method");
        }
        return MethodTypeDesc.ofDescriptor(asmMethod.getDescriptor());
    }

    public static MethodTypeDesc methodDesc(final String descriptor) {
        return MethodTypeDesc.ofDescriptor(descriptor);
    }

    public static ClassDesc classDesc(final String descriptor) {
        return ClassDesc.ofDescriptor(descriptor);
    }

    public static MethodTypeDesc replaceParameters(final MethodTypeDesc descriptor, final Predicate<? super ClassDesc> oldParam, final ClassDesc newParam) {
        return replaceParameters(descriptor, oldParam, newParam, null);
    }

    public static MethodTypeDesc replaceParameters(MethodTypeDesc descriptor, final Predicate<? super ClassDesc> oldParam, final ClassDesc newParam, final @Nullable Set<? super Integer> positionCollector) {
        for (int i = 0; i < descriptor.parameterCount(); i++) {
            if (oldParam.test(descriptor.parameterType(i))) {
                descriptor = descriptor.changeParameterType(i, newParam);
                if (positionCollector != null) positionCollector.add(i);
            }
        }
        return descriptor;
    }

    private DescriptorUtils() {
    }
}
