package io.papermc.asm.rules.method.generated;

import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.rules.generate.StaticHandlerGeneratedMethodHolder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;

import static io.papermc.asm.util.DescriptorUtils.replaceParameters;
import static java.util.function.Predicate.isEqual;

public interface TargetedTypeGeneratedStaticRewrite extends GeneratedStaticRewrite {

    /**
     * Gets the "new" type that exists in the current source.
     *
     * @return the "new" type from source
     */
    ClassDesc existingType();

    interface Parameter extends TargetedTypeGeneratedStaticRewrite, StaticHandlerGeneratedMethodHolder.Param {

        /**
         * Gets the targeted method matcher for the rewrite.
         *
         * @return the targeted method matcher
         */
        TargetedMethodMatcher methodMatcher();

        @Override
        default MethodTypeDesc transformInvokedDescriptor(final MethodTypeDesc original, final Set<Integer> context) {
            // To create the generated method descriptor from the existing (in source) descriptor, we replace the
            // legacy param with the new param
            return replaceParameters(original, isEqual(this.methodMatcher().targetType()), this.existingType(), context);
        }
    }

    interface Return extends TargetedTypeGeneratedStaticRewrite, StaticHandlerGeneratedMethodHolder.Return {

        @Override
        default MethodTypeDesc transformInvokedDescriptor(final MethodTypeDesc original, final Void context) {
            return original.changeReturnType(this.existingType());
        }
    }
}
