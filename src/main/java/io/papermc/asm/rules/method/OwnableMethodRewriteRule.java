package io.papermc.asm.rules.method;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.OwnableRewriteRule;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;

/**
 * A rule that targets specific owners.
 */
public interface OwnableMethodRewriteRule extends MethodRewriteRule, OwnableRewriteRule {

    @Override
    default boolean shouldProcess(final ClassProcessingContext context, final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface, final boolean isInvokeDynamic) {
        return this.matchesOwner(context, owner);
    }

    /**
     * A rule that targets specific owners and methods.
     */
    interface Filtered extends OwnableMethodRewriteRule {

        /**
         * A matcher to test against method names and descriptors.
         *
         * @return the method matcher
         */
        MethodMatcher methodMatcher();

        @Override
        default boolean shouldProcess(final ClassProcessingContext context, final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface, final boolean isInvokeDynamic) {
            return OwnableMethodRewriteRule.super.shouldProcess(context, opcode, owner, name, descriptor, isInterface, isInvokeDynamic)
                && this.methodMatcher().matches(opcode, isInvokeDynamic, name, descriptor);
        }
    }
}
