package io.papermc.asm.rules.method.params;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.rules.method.OwnableMethodRewriteRule;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import io.papermc.asm.rules.method.rewrite.SimpleRewrite;
import io.papermc.asm.versioned.ApiVersion;
import io.papermc.asm.versioned.VersionedRuleFactory;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;

import static io.papermc.asm.util.DescriptorUtils.replaceParameters;
import static java.util.function.Predicate.isEqual;

/**
 * Changes a parameter type to a super type. This isn't a compile break, but it is an ABI break. We just change the
 * offending parameter in the descriptor and move on.
 *
 * @param owners        owners of the methods to change
 * @param methodMatcher method matcher to find methods with (target is the type to be found in bytecode that needs to be transformed)
 * @param newParamType  the parameter type that is valid for existing method
 */
public record SuperTypeParamRewrite(Set<ClassDesc> owners, TargetedMethodMatcher methodMatcher, ClassDesc newParamType) implements OwnableMethodRewriteRule.Filtered {

    public ClassDesc oldParamType() {
        return this.methodMatcher.targetType();
    }

    @Override
    public MethodRewrite<?> rewrite(final ClassProcessingContext context, final boolean isInvokeDynamic, final int opcode, final ClassDesc owner, final String name, final MethodTypeDesc descriptor, final boolean isInterface) {
        return new SimpleRewrite(opcode, owner, name, this.modifyMethodDescriptor(descriptor), isInterface, isInvokeDynamic);
    }

    private MethodTypeDesc modifyMethodDescriptor(final MethodTypeDesc methodDescriptor) {
        return replaceParameters(methodDescriptor, isEqual(this.methodMatcher().targetType()), this.newParamType());
    }

    public record Versioned(Set<ClassDesc> owners, ClassDesc newParamType, VersionedMatcher<TargetedMethodMatcher> versions) implements VersionedRuleFactory {

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            return this.versions.ruleForVersion(apiVersion, matcher -> new SuperTypeParamRewrite(this.owners(), matcher, this.newParamType()));
        }
    }
}
