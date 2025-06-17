package io.papermc.asm.rules.method.params;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.rules.method.OwnableMethodRewriteRule;
import io.papermc.asm.rules.method.generated.TargetedTypeGeneratedStaticRewrite;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import io.papermc.asm.versioned.ApiVersion;
import io.papermc.asm.versioned.VersionedRuleFactory;
import io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.util.Set;
import org.objectweb.asm.Type;

import static io.papermc.asm.util.DescriptorUtils.replaceParameters;
import static java.util.function.Predicate.isEqual;

/**
 * Rewrites matching bytecode to a generated method. The generated method will accept {@link Object} params where the legacy type was found.
 * This is what gives it its "fuzziness". The usefulness of this is when only *some* valid parameters need to be converted to a new type,
 * and they don't share inheritance anymore.
 *
 * @param owners        the owners to target
 * @param existingType  the type to convert to
 * @param methodMatcher the method matcher to use which targets the legacy param type
 * @param staticHandler the method which will be used to convert the legacy type to the new type
 */
public record FuzzyParameterRewrite(Set<ClassDesc> owners, ClassDesc existingType, TargetedMethodMatcher methodMatcher, Method staticHandler) implements TargetedTypeGeneratedStaticRewrite.Parameter, OwnableMethodRewriteRule.Filtered {

    @Override
    public MethodTypeDesc transformToRedirectDescriptor(final MethodTypeDesc intermediateDescriptor) {
        // We need to replace the parameters in the bytecode descriptor that match the target with the fuzzy param
        return replaceParameters(intermediateDescriptor, isEqual(this.methodMatcher().targetType()), ConstantDescs.CD_Object);
    }

    @Override
    public MethodTypeDesc transformInvokedDescriptor(final MethodTypeDesc original, final Set<Integer> context) {
        // The "original" has already been made fuzzy by replacing specific params with Object.
        // To create the generated method descriptor from the descriptor, we replace the
        // fuzzy type with the fuzzy param
        return replaceParameters(original, isEqual(ConstantDescs.CD_Object), this.existingType(), context);
    }

    @Override
    public MethodRewrite<MethodCallData> createRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final MethodCallData originalCallData) {
        return Parameter.super.createRewrite(context, intermediateDescriptor, originalCallData)
            // so here's what's happening...
            // the dynamicMethodType needs to match the parameters being passed in at
            // runtime. The dynamicReturnType is run through the redirect transformer
            // to replace correct parameters.
            .withHandleExtras(arguments -> {
                final Type dynamicMethodType = (Type) arguments[MethodRewrite.DYNAMIC_TYPE_IDX];
                final MethodTypeDesc newDynamicMethodType = this.transformToRedirectDescriptor(MethodTypeDesc.ofDescriptor(dynamicMethodType.getDescriptor()));
                arguments[MethodRewrite.DYNAMIC_TYPE_IDX] = Type.getMethodType(newDynamicMethodType.descriptorString());
            });
    }

    public record Versioned(Set<ClassDesc> owners, ClassDesc existingType, VersionedMatcher<TargetedMethodMatcherWithHandler> versions) implements VersionedRuleFactory {

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            return this.versions.ruleForVersion(apiVersion, pair -> new FuzzyParameterRewrite(this.owners, this.existingType, pair.matcher(), pair.staticHandler()));
        }
    }
}
