package io.papermc.asm.rules.method.returns;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.rules.method.OwnableMethodRewriteRule;
import io.papermc.asm.rules.method.generated.TargetedTypeGeneratedStaticRewrite;
import io.papermc.asm.versioned.ApiVersion;
import io.papermc.asm.versioned.VersionedRuleFactory;
import io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Method;
import java.util.Set;

import static io.papermc.asm.util.DescriptorUtils.desc;

// Uses the methodMatcher against bytecode from plugins. Any matching descriptors will have the method name/owner changed to point towards
// a generated method of the same descriptor. That generated method will call the original method and pass the return value
// to staticHandler. staticHandler will then convert the object to the plugin bytecode's expected type.
public record DirectReturnRewrite(Set<ClassDesc> owners, ClassDesc existingType, TargetedMethodMatcher methodMatcher, Method staticHandler, boolean includeOwnerContext) implements TargetedTypeGeneratedStaticRewrite.Return, OwnableMethodRewriteRule.Filtered {

    public DirectReturnRewrite {
        if (includeOwnerContext && owners.size() > 1) {
            throw new IllegalArgumentException("Can't include owner context with multiple owners");
        }
        final ClassDesc owner = owners.iterator().next();
        if (!desc(staticHandler.getReturnType()).equals(methodMatcher.targetType())) {
            throw new IllegalArgumentException("Return type of staticHandler doesn't match target from methodMatcher");
        }
        final int expectedStaticHandlerParamCount = includeOwnerContext ? 2 : 1;
        if (staticHandler.getParameterCount() != expectedStaticHandlerParamCount) {
            throw new IllegalArgumentException("staticHandler should only have %s parameter(s) of type %s".formatted(expectedStaticHandlerParamCount, (includeOwnerContext ? owner + " and " : "") + methodMatcher.targetType()));
        }
        if (!staticHandler.getParameterTypes()[includeOwnerContext ? 1 : 0].describeConstable().orElseThrow().equals(existingType)) {
            throw new IllegalArgumentException("staticHandler param type isn't " + existingType);
        }
    }

    public record Versioned(Set<ClassDesc> owners, ClassDesc existingType, VersionedMatcher<TargetedMethodMatcherWithHandler> versions, boolean includeOwnerContext) implements VersionedRuleFactory {

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            return this.versions.ruleForVersion(apiVersion, pair -> new DirectReturnRewrite(this.owners(), this.existingType(), pair.matcher(), pair.staticHandler(), this.includeOwnerContext()));
        }
    }
}
