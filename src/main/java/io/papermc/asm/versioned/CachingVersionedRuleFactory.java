package io.papermc.asm.versioned;

import io.papermc.asm.rules.RewriteRule;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus;

/**
 * Caches creating {@link RewriteRule}s for each {@link io.papermc.asm.versioned.ApiVersion}.
 */
public abstract class CachingVersionedRuleFactory implements io.papermc.asm.versioned.VersionedRuleFactory {

    private final Map<io.papermc.asm.versioned.ApiVersion<?>, RewriteRule> cache = new ConcurrentHashMap<>();
    private io.papermc.asm.versioned.VersionedRuleFactory rootFactory;

    @ApiStatus.OverrideOnly
    public abstract io.papermc.asm.versioned.VersionedRuleFactory createRootFactory();

    protected final VersionedRuleFactory rootFactory() {
        if (this.rootFactory == null) {
            this.rootFactory = this.createRootFactory();
        }
        return this.rootFactory;
    }

    @Override
    public final RewriteRule createRule(final ApiVersion<?> apiVersion) {
        return this.cache.computeIfAbsent(apiVersion, this.rootFactory()::createRule);
    }
}
