package io.papermc.asm.rules.builder.matcher.method;

import java.lang.constant.MethodTypeDesc;
import java.util.function.Consumer;
import java.util.function.Predicate;

@FunctionalInterface
public interface MethodMatcher {

    static MethodMatcherBuilder builder() {
        return new MethodMatcherBuilderImpl();
    }

    static MethodMatcher single(final String name, final Consumer<MethodMatcherBuilder.MatchBuilder> matchBuilderConsumer) {
        return builder().match(name, matchBuilderConsumer).build();
    }

    boolean matches(int opcode, boolean isInvokeDynamic, String name, String descriptor);

    default boolean matches(final int opcode, final boolean isInvokeDynamic, final String name, final MethodTypeDesc descriptor) {
        return this.matches(opcode, isInvokeDynamic, name, descriptor.descriptorString());
    }

    /**
     * Creates a method matcher that matches if either matcher passes.
     *
     * @param other the other matcher
     * @return a new "or" matcher
     */
    default MethodMatcher or(final MethodMatcher other) {
        return (opcode, isInvokeDynamic, name, descriptor) -> this.matches(opcode, isInvokeDynamic, name, descriptor) || other.matches(opcode, isInvokeDynamic, name, descriptor);
    }

    /**
     * Creates a method matcher that matches if both matcher pass.
     *
     * @param other the other matcher
     * @return a new "and" matcher
     * @see #and(Predicate)
     */
    default MethodMatcher and(final MethodMatcher other) {
        return (opcode, isInvokeDynamic, name, descriptor) -> this.matches(opcode, isInvokeDynamic, name, descriptor) && other.matches(opcode, isInvokeDynamic, name, descriptor);
    }

    /**
     * Creates a method matcher tha matches if both this matcher
     * and the method descriptor predicate pass.
     *
     * @param descPredicate the method descriptor predicate
     * @return a new "and" matcher
     */
    default MethodMatcher and(final Predicate<? super MethodTypeDesc> descPredicate) {
        return (opcode, isInvokeDynamic, name, descriptor) -> this.matches(opcode, isInvokeDynamic, name, descriptor) && descPredicate.test(MethodTypeDesc.ofDescriptor(descriptor));
    }

    /**
     * Creates a method matcher that is the inverse of this matcher.
     *
     * @return the inverse matcher
     */
    default MethodMatcher negate() {
        return (opcode, isInvokeDynamic, name, descriptor) -> !this.matches(opcode, isInvokeDynamic, name, descriptor);
    }
}
