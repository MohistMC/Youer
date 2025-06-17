package io.papermc.asm.rules.method;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import io.papermc.asm.rules.method.rewrite.ConstructorRewrite;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import io.papermc.asm.rules.method.rewrite.SimpleRewrite;
import io.papermc.asm.versioned.ApiVersion;
import io.papermc.asm.versioned.VersionedRuleFactory;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.papermc.asm.util.DescriptorUtils.toOwner;
import static io.papermc.asm.util.OpcodeUtils.staticOp;

/**
 * Rewrites a method by just directly routing it to an identical static method in another class.
 *
 * @param owners              the owners to target
 * @param methodMatcher       the method matcher to use
 * @param staticRedirectOwner the owner to redirect to
 */
public record DirectStaticRewrite(Set<ClassDesc> owners, @Nullable String staticMethodName, MethodMatcher methodMatcher, ClassDesc staticRedirectOwner) implements io.papermc.asm.rules.method.StaticRewrite, OwnableMethodRewriteRule.Filtered {

    public DirectStaticRewrite(final Set<ClassDesc> owners, final MethodMatcher methodMatcher, final ClassDesc staticRedirectOwner) {
        this(owners, null, methodMatcher, staticRedirectOwner);
    }

    @Override
    public MethodRewrite<GeneratedMethodHolder.MethodCallData> createRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final GeneratedMethodHolder.MethodCallData originalCallData) {
        if (this.staticMethodName() == null) {
            return io.papermc.asm.rules.method.StaticRewrite.super.createRewrite(context, intermediateDescriptor, originalCallData);
        } else {
            return new SimpleRewrite(staticOp(originalCallData.isInvokeDynamic()), this.staticRedirectOwner(context), this.staticMethodName(), this.transformToRedirectDescriptor(intermediateDescriptor), false, originalCallData.isInvokeDynamic());
        }
    }

    @Override
    public MethodRewrite<GeneratedMethodHolder.ConstructorCallData> createConstructorRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final GeneratedMethodHolder.ConstructorCallData originalCallData) {
        if (this.staticMethodName() == null) {
            return StaticRewrite.super.createConstructorRewrite(context, intermediateDescriptor, originalCallData);
        } else {
            return new ConstructorRewrite(this.staticRedirectOwner(context), toOwner(originalCallData.owner()), this.staticMethodName(), this.transformToRedirectDescriptor(intermediateDescriptor));
        }
    }

    @Override
    public ClassDesc staticRedirectOwner(final ClassProcessingContext context) {
        return this.staticRedirectOwner;
    }

    public record Versioned(Set<ClassDesc> owners, ClassDesc staticRedirectOwner, @Nullable String staticMethodName, VersionedMatcher<MethodMatcher> versions) implements VersionedRuleFactory {

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            return this.versions.ruleForVersion(apiVersion, match -> new DirectStaticRewrite(this.owners(), this.staticMethodName(), match, this.staticRedirectOwner()));
        }
    }
}
