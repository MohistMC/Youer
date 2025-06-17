package io.papermc.asm.rules.builder.matcher.method;

import io.papermc.asm.rules.method.StaticRewrite;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

class MethodMatcherBuilderImpl implements io.papermc.asm.rules.builder.matcher.method.MethodMatcherBuilder {
    private io.papermc.asm.rules.builder.matcher.method.MethodMatcher matcher = (o, isInvokeDynamic, n, d) -> false;

    MethodMatcherBuilderImpl() {
    }

    @Override
    public MethodMatcher build() {
        return this.matcher;
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.MethodMatcherBuilder ctor(final Consumer<MatchBuilder> matchBuilderConsumer) {
        final Consumer<MatchBuilder> ctorConsumer = b -> b.type(io.papermc.asm.rules.builder.matcher.method.MethodType.SPECIAL);
        return this.match(StaticRewrite.CONSTRUCTOR_METHOD_NAME, ctorConsumer.andThen(matchBuilderConsumer));
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.MethodMatcherBuilder match(final String name, final Consumer<MatchBuilder> matchBuilderConsumer) {
        return this.match(Collections.singleton(name), matchBuilderConsumer);
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.MethodMatcherBuilder match(final Collection<String> names, final Consumer<MatchBuilder> matchBuilderConsumer) {
        final Collection<String> namesCopy = Set.copyOf(names);
        final SpecificMatchBuilder matchBuilder = new SpecificMatchBuilder(namesCopy::contains);
        matchBuilderConsumer.accept(matchBuilder);
        matchBuilder.apply();
        return this;
    }

    @Override
    public MethodMatcherBuilder desc(final Predicate<? super MethodTypeDesc> descPredicate) {
        this.matcher = this.matcher.and(descPredicate);
        return this;
    }

    final class SpecificMatchBuilder implements MatchBuilder {

        private final Predicate<String> namePredicate;
        private Predicate<? super MethodTypeDesc> bytecodeDescPredicate = $ -> true;
        private BiPredicate<Integer, Boolean> opcodePredicate = ($, $$) -> true;

        private SpecificMatchBuilder(final Predicate<String> namePredicate) {
            this.namePredicate = namePredicate;
        }

        private void apply() {
            MethodMatcherBuilderImpl.this.matcher = MethodMatcherBuilderImpl.this.matcher.or((o, isInvokeDynamic, n, d) -> {
                return this.namePredicate.test(n) && this.opcodePredicate.test(o, isInvokeDynamic) && this.bytecodeDescPredicate.test(MethodTypeDesc.ofDescriptor(d));
            });
        }

        @Override
        public MatchBuilder type(final MethodType...types) {
            this.opcodePredicate = (o, b) -> Arrays.stream(types).anyMatch(type -> type.matches(o, b));
            return this;
        }

        @Override
        public MatchBuilder desc(final Predicate<? super MethodTypeDesc> descPredicate) {
            this.bytecodeDescPredicate = descPredicate;
            return this;
        }
    }
}
