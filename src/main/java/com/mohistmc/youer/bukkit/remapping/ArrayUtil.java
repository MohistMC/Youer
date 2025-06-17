package com.mohistmc.youer.bukkit.remapping;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;

public class ArrayUtil {


    public static Set<String> stringSet = new HashSet<>();

    public static Object[] append(Object[] array, Object obj) {
        Object[] newArray = new Object[array.length + 1];
        newArray[array.length] = obj;
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static Object[] prepend(Object[] array, Object obj) {
        Object[] newArray = new Object[array.length + 1];
        newArray[0] = obj;
        System.arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }

    public static <T> T[] prepend(T[] array, T obj, IntFunction<T[]> factory) {
        T[] newArray = factory.apply(array.length + 1);
        newArray[0] = obj;
        System.arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }
}