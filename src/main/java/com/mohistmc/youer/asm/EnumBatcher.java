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
        return addAll(clazz, names, paramTypes, args, -1);
    }

    /**
     * @param insertAt index the new constants are placed at; -1 appends. Constants after the
     *                 insertion point get their {@link Enum#ordinal()} shifted so it still
     *                 matches the array index, which plugins index tables by.
     */
    public static <T> List<T> addAll(Class<T> clazz, List<String> names, List<Class<?>> paramTypes, List<List<Object>> args, int insertAt) {
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
            int at = insertAt < 0 ? old.length : Math.max(0, Math.min(insertAt, old.length));
            Object[] grown = (Object[]) Array.newInstance(clazz, old.length + names.size());
            System.arraycopy(old, 0, grown, 0, at);

            MethodHandle ctor = constructor(clazz, paramTypes);
            List<T> added = new ArrayList<>(names.size());
            List<Object> callArgs = new ArrayList<>(paramTypes.size() + 2);
            for (int i = 0; i < names.size(); i++) {
                callArgs.clear();
                callArgs.add(normalize(names.get(i)));
                callArgs.add(at + i);
                callArgs.addAll(args.get(i));
                T instance = clazz.cast(ctor.invokeWithArguments(callArgs));
                grown[at + i] = instance;
                added.add(instance);
            }
            System.arraycopy(old, at, grown, at + names.size(), old.length - at);

            Unsafe.putObject(base, offset, grown);
            shiftOrdinals(old, at, names.size());
            cleanEnumCache(clazz);
            return added;
        } catch (Throwable t) {
            Implementer.LOGGER.warn("Batch enum injection failed for {}, falling back", clazz, t);
            return null;
        }
    }

    private static long enumOrdinalOffset = -2;

    // the tail of the values array moved right, so the moved constants need their ordinal rewritten
    private static void shiftOrdinals(Object[] old, int from, int shift) {
        if (shift == 0 || from >= old.length) {
            return;
        }
        if (enumOrdinalOffset == -2) {
            try {
                enumOrdinalOffset = Unsafe.getUnsafe().objectFieldOffset(Enum.class.getDeclaredField("ordinal"));
            } catch (Throwable t) {
                enumOrdinalOffset = -1;
                Implementer.LOGGER.warn("Cannot shift enum ordinals", t);
            }
        }
        if (enumOrdinalOffset < 0) {
            return;
        }
        for (int i = from; i < old.length; i++) {
            Unsafe.getUnsafe().putInt(old[i], enumOrdinalOffset, i + shift);
        }
    }

    public static <T> T addAt(Class<T> clazz, String name, List<Class<?>> paramTypes, List<Object> args, int insertAt) {
        List<T> added = addAll(clazz, List.of(name), paramTypes, List.of(args), insertAt);
        return added == null ? null : added.getFirst();
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
