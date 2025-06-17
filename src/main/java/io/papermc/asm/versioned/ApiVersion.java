package io.papermc.asm.versioned;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.OverrideOnly
public interface ApiVersion<I extends ApiVersion<I>> extends Comparable<I> {

    default boolean isNewerThan(final I apiVersion) {
        return this.compareTo(apiVersion) > 0;
    }

    default boolean isOlderThan(final I apiVersion) {
        return this.compareTo(apiVersion) < 0;
    }

    default boolean isNewerThanOrSameAs(final I apiVersion) {
        return this.compareTo(apiVersion) >= 0;
    }

    default boolean isOlderThanOrSameAs(final I apiVersion) {
        return this.compareTo(apiVersion) <= 0;
    }
}
