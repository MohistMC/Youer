package io.papermc.asm.versioned.matcher;

import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.versioned.ApiVersion;
import java.lang.reflect.Method;
import org.jetbrains.annotations.Contract;

public interface VersionedTargetedMethodMatcherWithHandlerBuilder extends VersionedMatcherBuilder<io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler> {

    @Contract(value = "_, _, _ -> this", mutates = "this")
    VersionedTargetedMethodMatcherWithHandlerBuilder with(ApiVersion<?> apiVersion, TargetedMethodMatcher matcher, Method staticHandler);

    @Override
    VersionedTargetedMethodMatcherWithHandlerBuilder with(ApiVersion<?> apiVersion, TargetedMethodMatcherWithHandler context);

}
