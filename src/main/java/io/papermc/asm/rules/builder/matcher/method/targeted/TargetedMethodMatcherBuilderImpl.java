package io.papermc.asm.rules.builder.matcher.method.targeted;

import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.builder.matcher.method.MethodType;
import io.papermc.asm.rules.builder.matcher.method.OpcodePredicate;
import io.papermc.asm.rules.method.StaticRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

class TargetedMethodMatcherBuilderImpl implements io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcherBuilder {

    private MethodMatcher matcher = (opcode, isInvokeDynamic, name, descriptor) -> false;
    private Predicate<MethodTypeDesc> byDesc = $ -> true;
    private @MonotonicNonNull ClassDesc oldType;

    TargetedMethodMatcherBuilderImpl() {
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcherBuilder ctor() {
        return this.match(StaticRewrite.CONSTRUCTOR_METHOD_NAME, b -> b.type(MethodType.SPECIAL));
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcherBuilder match(final String name, final Consumer<MatchBuilder> matchBuilderConsumer) {
        return this.match(Collections.singleton(name), matchBuilderConsumer);
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcherBuilder match(final Collection<String> names, final Consumer<MatchBuilder> matchBuilderConsumer) {
        final Collection<String> namesCopy = Set.copyOf(names);
        final SpecificMatchBuilder matchBuilder = new SpecificMatchBuilder(namesCopy::contains);
        matchBuilderConsumer.accept(matchBuilder);
        matchBuilder.apply();
        return this;
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcherBuilder targetParam(final ClassDesc classDesc) {
        if (this.oldType != null) {
            throw new IllegalArgumentException("Targeted type was already set to " + this.oldType);
        }
        this.oldType = classDesc;
        return this.hasParam(classDesc);
    }

    @Override
    public io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcherBuilder targetReturn(final ClassDesc classDesc) {
        if (this.oldType != null) {
            throw new IllegalArgumentException("Targeted type was already set to " + this.oldType);
        }
        this.oldType = classDesc;
        return this.hasReturn(classDesc);
    }

    @Override
    public TargetedMethodMatcherBuilder desc(final Predicate<? super MethodTypeDesc> descPredicate) {
        this.byDesc = this.byDesc.and(descPredicate);
        return this;
    }

    @Override
    public TargetedMethodMatcher build() {
        if (this.oldType == null) {
            throw new IllegalStateException("Targeted type was not set");
        }
        final MethodMatcher finalMatcher = this.matcher.and(this.byDesc);
        return new TargetedMethodMatcherImpl(finalMatcher, this.oldType);
    }

    final class SpecificMatchBuilder implements MatchBuilder {

        private final Predicate<String> namePredicate;
        private Predicate<? super MethodTypeDesc> bytecodeDescPredicate = $ -> true;
        private OpcodePredicate opcodePredicate = ($, $$) -> true;

        private SpecificMatchBuilder(final Predicate<String> namePredicate) {
            this.namePredicate = namePredicate;
        }

        private void apply() {
            TargetedMethodMatcherBuilderImpl.this.matcher = TargetedMethodMatcherBuilderImpl.this.matcher.or((o, isInvokeDynamic, n, d) -> {
                return this.namePredicate.test(n)
                    && this.opcodePredicate.matches(o, isInvokeDynamic)
                    && this.bytecodeDescPredicate.test(MethodTypeDesc.ofDescriptor(d));
            });
        }

        @Override
        public MatchBuilder type(final MethodType... types) {
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
