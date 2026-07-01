package com.mohistmc.youer.util;

import java.util.Iterator;
import org.jspecify.annotations.NonNull;

public record IterableWrapper<T>(Iterator<T> iterator) implements Iterable<T> {

    @Override
    public @NonNull Iterator<T> iterator() {
        return iterator;
    }

}
