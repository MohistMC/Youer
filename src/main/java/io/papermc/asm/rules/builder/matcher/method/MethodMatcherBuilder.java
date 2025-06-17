package io.papermc.asm.rules.builder.matcher.method;

import io.papermc.asm.util.Builder;
import java.util.Collection;
import java.util.function.Consumer;

public interface MethodMatcherBuilder extends MethodParamMatcherBuilder<MethodMatcherBuilder>, Builder<MethodMatcher> {

    MethodMatcherBuilder ctor(final Consumer<MatchBuilder> matchBuilderConsumer);

    default MethodMatcherBuilder match(final String name) {
        return this.match(name, b -> {});
    }

    MethodMatcherBuilder match(final String name, final Consumer<MatchBuilder> matchBuilderConsumer);

    default MethodMatcherBuilder match(final Collection<String> names) {
        return this.match(names, b -> {});
    }

    MethodMatcherBuilder match(final Collection<String> names, final Consumer<MatchBuilder> matchBuilderConsumer);

    /**
     * Used to match methods with specific names.
     *
     * @see MethodMatcherBuilder#match(String, Consumer)
     */
    interface MatchBuilder extends MethodParamMatcherBuilder<MatchBuilder>, MethodTypeMatcherBuilder<MatchBuilder> {
    }
}
