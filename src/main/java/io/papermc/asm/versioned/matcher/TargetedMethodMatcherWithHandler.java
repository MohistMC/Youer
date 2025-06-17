package io.papermc.asm.versioned.matcher;

import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import java.lang.reflect.Method;

public record TargetedMethodMatcherWithHandler(TargetedMethodMatcher matcher, Method staticHandler) {
}
