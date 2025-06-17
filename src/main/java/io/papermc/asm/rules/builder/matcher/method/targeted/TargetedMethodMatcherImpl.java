package io.papermc.asm.rules.builder.matcher.method.targeted;

import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import java.lang.constant.ClassDesc;

public class TargetedMethodMatcherImpl implements TargetedMethodMatcher {

    private final MethodMatcher wrapped;
    private final ClassDesc oldType;

    TargetedMethodMatcherImpl(final MethodMatcher wrapped, final ClassDesc oldType) {
        this.wrapped = wrapped;
        this.oldType = oldType;
    }

    @Override
    public boolean matches(final int opcode, final boolean isInvokeDynamic, final String name, final String descriptor) {
        return this.wrapped.matches(opcode, isInvokeDynamic, name, descriptor);
    }

    @Override
    public MethodMatcher negate() {
        return new TargetedMethodMatcherImpl(this.wrapped.negate(), this.oldType);
    }

    @Override
    public ClassDesc targetType() {
        return this.oldType;
    }
}
