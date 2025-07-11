package io.papermc.asm.versioned.matcher;

import io.papermc.asm.versioned.ApiVersion;
import java.util.NavigableMap;
import java.util.TreeMap;

public class VersionedMatcherBuilderImpl<C> implements io.papermc.asm.versioned.matcher.VersionedMatcherBuilder<C> {

    protected final NavigableMap<ApiVersion<?>, C> versions = new TreeMap<>();

    @Override
    public VersionedMatcherBuilder<C> with(final ApiVersion<?> apiVersion, final C context) {
        if (this.versions.containsKey(apiVersion)) {
            throw new IllegalArgumentException("Duplicate version: " + apiVersion);
        }
        this.versions.put(apiVersion, context);
        return this;
    }

    @Override
    public io.papermc.asm.versioned.matcher.VersionedMatcher<C> build() {
        return new VersionedMatcher<>(this.versions);
    }
}
