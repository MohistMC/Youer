package io.papermc.asm.versioned.matcher;

import io.papermc.asm.util.Builder;
import io.papermc.asm.versioned.ApiVersion;
import org.jetbrains.annotations.Contract;

public interface VersionedMatcherBuilder<C> extends Builder<VersionedMatcher<C>> {

    @Contract(value = "_, _ -> this", mutates = "this")
    VersionedMatcherBuilder<C> with(ApiVersion<?> apiVersion, C context);
}
