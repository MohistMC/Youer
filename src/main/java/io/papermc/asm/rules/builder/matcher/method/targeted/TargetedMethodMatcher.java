package io.papermc.asm.rules.builder.matcher.method.targeted;

import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import java.lang.constant.ClassDesc;

public interface TargetedMethodMatcher extends MethodMatcher {

    static TargetedMethodMatcherBuilder builder() {
        return new TargetedMethodMatcherBuilderImpl();
    }

    ClassDesc targetType();
}
