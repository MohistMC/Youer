package io.papermc.asm.rules.builder.matcher.method.targeted;

import io.papermc.asm.rules.builder.matcher.method.MethodParamMatcherBuilder;
import io.papermc.asm.rules.builder.matcher.method.MethodTypeMatcherBuilder;
import io.papermc.asm.util.Builder;
import java.lang.constant.ClassDesc;
import java.util.Collection;
import java.util.function.Consumer;

public interface TargetedMethodMatcherBuilder extends MethodParamMatcherBuilder<TargetedMethodMatcherBuilder>, Builder<TargetedMethodMatcher> {

    TargetedMethodMatcherBuilder ctor();

    default TargetedMethodMatcherBuilder match(final String name) {
        return this.match(name, b -> {});
    }

    TargetedMethodMatcherBuilder match(final String name, final Consumer<MatchBuilder> matchBuilderConsumer);

    default TargetedMethodMatcherBuilder match(final Collection<String> names) {
        return this.match(names, b -> {});
    }

    TargetedMethodMatcherBuilder match(final Collection<String> names, final Consumer<MatchBuilder> matchBuilderConsumer);

    TargetedMethodMatcherBuilder targetParam(final ClassDesc paramClassDesc);

    TargetedMethodMatcherBuilder targetReturn(final ClassDesc returnClassDesc);

    interface MatchBuilder extends MethodParamMatcherBuilder<MatchBuilder>, MethodTypeMatcherBuilder<MatchBuilder> {
    }
}
