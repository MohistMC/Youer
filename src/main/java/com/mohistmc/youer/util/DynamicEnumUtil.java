package com.mohistmc.youer.util;

import com.mohistmc.youer.Youer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

/**
 * Adds enum constants at runtime on JDK 25.
 * <p>
 * The old dynamic enum helper called {@code Lookup.ensureInitialized} through the JDK
 * trusted lookup, which throws an access violation for classes in the unnamed module on
 * JDK 25. This helper initializes the enum with
 * {@link Class#forName(String, boolean, ClassLoader)} and resolves the constructor with a
 * private lookup instead.
 *
 * @author Mgazul
 */
public final class DynamicEnumUtil {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private DynamicEnumUtil() {
    }

    /**
     * Normalizes an identifier to a valid enum constant name.
     */
    public static String normalizeName(String name) {
        return name.replace(':', '_')
                .replaceAll("\\s+", "_")
                .replaceAll("\\W", "")
                .toUpperCase(Locale.ENGLISH);
    }

    /**
     * Appends a constant to an enum.
     *
     * @param enumClass the enum class
     * @param name      the new constant name
     * @return the new constant, or {@code null} if it could not be created
     */
    public static <T extends Enum<T>> @Nullable T addEnum(Class<T> enumClass, String name) {
        return addEnum(enumClass, name, new Class<?>[0], new Object[0]);
    }

    /**
     * Appends a constant to an enum.
     *
     * @param enumClass      the enum class
     * @param name           the new constant name
     * @param parameterTypes the enum constructor parameter types (without the implicit name/ordinal)
     * @param arguments      the enum constructor arguments (without the implicit name/ordinal)
     * @return the new constant, or {@code null} if it could not be created
     */
    public static <T extends Enum<T>> @Nullable T addEnum(Class<T> enumClass, String name, List<Class<?>> parameterTypes, List<Object> arguments) {
        return addEnum(enumClass, name, parameterTypes.toArray(new Class<?>[0]), arguments.toArray());
    }

    /**
     * Appends a constant to an enum.
     *
     * @param enumClass      the enum class
     * @param name           the new constant name
     * @param parameterTypes the enum constructor parameter types (without the implicit name/ordinal)
     * @param arguments      the enum constructor arguments (without the implicit name/ordinal)
     * @return the new constant, or {@code null} if it could not be created
     */
    public static <T extends Enum<T>> @Nullable T addEnum(Class<T> enumClass, String name, Class<?>[] parameterTypes, Object[] arguments) {
        try {
            // Force the enum's static initializer without the broken trusted-lookup access check.
            Class.forName(enumClass.getName(), true, enumClass.getClassLoader());

            Class<?>[] constructorTypes = new Class<?>[parameterTypes.length + 2];
            constructorTypes[0] = String.class;
            constructorTypes[1] = int.class;
            System.arraycopy(parameterTypes, 0, constructorTypes, 2, parameterTypes.length);

            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(enumClass, MethodHandles.lookup());
            MethodHandle constructor = lookup.findConstructor(enumClass, MethodType.methodType(void.class, constructorTypes));

            Field valuesField = findValuesField(enumClass);
            if (valuesField == null) {
                Youer.LOGGER.warn("Enum {} has no $VALUES field", enumClass.getName());
                return null;
            }

            Object valuesBase = UNSAFE.staticFieldBase(valuesField);
            long valuesOffset = UNSAFE.staticFieldOffset(valuesField);
            Object[] oldValues = (Object[]) UNSAFE.getObject(valuesBase, valuesOffset);
            Object[] newValues = (Object[]) Array.newInstance(enumClass, oldValues.length + 1);
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);

            Object[] constructorArguments = new Object[arguments.length + 2];
            constructorArguments[0] = name;
            constructorArguments[1] = oldValues.length;
            System.arraycopy(arguments, 0, constructorArguments, 2, arguments.length);

            T constant = enumClass.cast(constructor.invokeWithArguments(constructorArguments));
            newValues[oldValues.length] = constant;
            UNSAFE.putObject(valuesBase, valuesOffset, newValues);

            clearEnumCache(enumClass);
            return constant;
        } catch (Throwable t) {
            Youer.LOGGER.warn("Failed to add enum constant " + name + " to " + enumClass.getName(), t);
            return null;
        }
    }

    private static @Nullable Field findValuesField(Class<?> enumClass) {
        for (Field field : enumClass.getDeclaredFields()) {
            if (field.getName().equals("$VALUES") || field.getName().equals("ENUM$VALUES")) {
                return field;
            }
        }
        return null;
    }

    private static void clearEnumCache(Class<?> enumClass) {
        for (String fieldName : new String[]{"enumConstantDirectory", "enumConstants", "enumVars"}) {
            try {
                Field field = Class.class.getDeclaredField(fieldName);
                UNSAFE.putObject(enumClass, UNSAFE.objectFieldOffset(field), null);
            } catch (NoSuchFieldException ignored) {
                // field does not exist on this JDK
            } catch (Throwable t) {
                Youer.LOGGER.warn("Failed to clear enum cache field " + fieldName, t);
            }
        }
    }
}
