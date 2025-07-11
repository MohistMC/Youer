package io.papermc.asm.versioned.matcher;

import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.versioned.ApiVersion;
import java.lang.reflect.Method;

public final class VersionedTargetedMethodMatcherWithHandlerBuilderImpl
    extends VersionedMatcherBuilderImpl<io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler>
    implements io.papermc.asm.versioned.matcher.VersionedTargetedMethodMatcherWithHandlerBuilder {

    @Override
    public io.papermc.asm.versioned.matcher.VersionedTargetedMethodMatcherWithHandlerBuilder with(final ApiVersion<?> apiVersion, final io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler context) {
        return (io.papermc.asm.versioned.matcher.VersionedTargetedMethodMatcherWithHandlerBuilder) super.with(apiVersion, context);
    }

    @Override
    public VersionedTargetedMethodMatcherWithHandlerBuilder with(final ApiVersion<?> apiVersion, final TargetedMethodMatcher matcher, final Method staticHandler) {
        return this.with(apiVersion, new TargetedMethodMatcherWithHandler(matcher, staticHandler));
    }
}
