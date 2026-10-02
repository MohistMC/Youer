package com.mohistmc.youer.asm;

import com.mohistmc.youer.bukkit.remapping.Unsafe;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Single entry point for injecting enum constants, self-contained.
 * Adding constants one by one grows and republishes the values array per call, which is O(n^2)
 * once a registry reaches tens of thousands of entries; {@link #addAll} does it in one pass.
 */
public final class EnumBatcher {

    private static final List<String> ENUM_CACHE_FIELDS = List.of("enumConstantDirectory", "enumConstants", "enumVars");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_WORD = Pattern.compile("\\W");

    private EnumBatcher() {
    }

    /**
     * Single constant, same path as {@link #addAll}.
     */
    public static <T> T add(Class<T> clazz, String name, List<Class<?>> paramTypes, List<Object> args) {
        List<T> added = addAll(clazz, List.of(name), paramTypes, List.of(args));
        return added == null ? null : added.getFirst();
    }

    /**
     * Single constant without extra constructor parameters.
     */
    public static <T> T add(Class<T> clazz, String name) {
        return add(clazz, name, List.of(), List.of());
    }

    /**
     * Same normalization the old library used, but with the patterns compiled once.
     */
    public static String normalizeName(String name) {
        return NON_WORD.matcher(WHITESPACE.matcher(name.replace(':', '_')).replaceAll("_"))
                .replaceAll("")
                .toUpperCase(Locale.ENGLISH);
    }

    /**
     * @return the created constants, or null when the batch could not be applied (caller should fall back)
     */
    public static <T> List<T> addAll(Class<T> clazz, List<String> names, List<Class<?>> paramTypes, List<List<Object>> args) {
        if (names.isEmpty()) {
            return List.of();
        }
        try {
            Unsafe.lookup().ensureInitialized(clazz);
            Field values = valuesField(clazz);
            if (values == null) {
                return null;
            }
            Object base = Unsafe.staticFieldBase(values);
            long offset = Unsafe.staticFieldOffset(values);
            Object[] old = (Object[]) Unsafe.getObject(base, offset);
            Object[] grown = (Object[]) Array.newInstance(clazz, old.length + names.size());
            System.arraycopy(old, 0, grown, 0, old.length);

            MethodHandle ctor = constructor(clazz, paramTypes);
            List<T> added = new ArrayList<>(names.size());
            List<Object> callArgs = new ArrayList<>(paramTypes.size() + 2);
            for (int i = 0; i < names.size(); i++) {
                callArgs.clear();
                callArgs.add(normalize(names.get(i)));
                callArgs.add(old.length + i);
                callArgs.addAll(args.get(i));
                T instance = clazz.cast(ctor.invokeWithArguments(callArgs));
                grown[old.length + i] = instance;
                added.add(instance);
            }

            Unsafe.putObject(base, offset, grown);
            cleanEnumCache(clazz);
            return added;
        } catch (Throwable t) {
            Implementer.LOGGER.warn("Batch enum injection failed for {}, falling back", clazz, t);
            return null;
        }
    }

    // names are usually normalized by the caller already; skip the regex round-trip in that case
    private static String normalize(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != '_' && (c < 'A' || c > 'Z') && (c < '0' || c > '9')) {
                return normalizeName(name);
            }
        }
        return name;
    }

    private static Field valuesField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals("$VALUES") || field.getName().equals("ENUM$VALUES")) {
                return field;
            }
        }
        return null;
    }

    private static MethodHandle constructor(Class<?> clazz, List<Class<?>> paramTypes) throws ReflectiveOperationException {
        List<Class<?>> types = new ArrayList<>(paramTypes.size() + 2);
        types.add(String.class);
        types.add(Integer.TYPE);
        types.addAll(paramTypes);
        return Unsafe.lookup().findConstructor(clazz, MethodType.methodType(Void.TYPE, types));
    }

    // drop the caches Class keeps per enum so values()/valueOf() see the new constants
    private static void cleanEnumCache(Class<?> clazz) {
        for (String name : ENUM_CACHE_FIELDS) {
            try {
                Field field = Class.class.getDeclaredField(name);
                Unsafe.putObjectVolatile(clazz, Unsafe.objectFieldOffset(field), null);
            } catch (NoSuchFieldException ignored) {
            }
        }
    }
}
