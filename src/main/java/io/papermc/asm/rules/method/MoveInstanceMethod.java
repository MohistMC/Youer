package io.papermc.asm.rules.method;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.method.generated.GeneratedStaticRewrite;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import io.papermc.asm.versioned.ApiVersion;
import io.papermc.asm.versioned.VersionedRuleFactory;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static io.papermc.asm.util.OpcodeUtils.isInterface;
import static io.papermc.asm.util.OpcodeUtils.isStatic;

/**
 * Generally used to move a method from an interface to its implementation class. This is
 * useful when the API has a changed method, but you still want the exact behavior of the
 * original method. Just keep the original in the implementation and redirect to the implementation.
 */
public record MoveInstanceMethod(
    Set<ClassDesc> owners,
    MethodMatcher methodMatcher,
    ClassDesc newOwner,
    String newMethodName
) implements GeneratedStaticRewrite, OwnableMethodRewriteRule.Filtered {

    @Override
    public @Nullable MethodRewrite<?> rewrite(final ClassProcessingContext context, final boolean isInvokeDynamic, final int opcode, final ClassDesc owner, final String name, final MethodTypeDesc descriptor, final boolean isInterface) {
        if (!isStatic(opcode, isInvokeDynamic) && !isInterface(opcode, isInvokeDynamic)) {
            throw new IllegalArgumentException("Cannot use " + this + " for " + opcode);
        }
        return GeneratedStaticRewrite.super.rewrite(context, isInvokeDynamic, opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void generateMethod(final GeneratorAdapterFactory factory, final MethodCallData modified, final MethodCallData original) {
        final GeneratorAdapter methodGenerator = this.createAdapter(factory, modified);
        for (int i = 0; i < modified.descriptor().parameterCount(); i++) {
            methodGenerator.loadArg(i);
            if (i == 0) {
                // change type to new owner when first param (which for invokeinterface and invokevirtual is the owner object)
                methodGenerator.checkCast(Type.getType(this.newOwner().descriptorString()));
            }
        }
        final Type newOwnerType = Type.getType(this.newOwner().descriptorString());
        final Method newMethodType = new Method(this.newMethodName(), original.descriptor().descriptorString());
        methodGenerator.invokeVirtual(newOwnerType, newMethodType);
        methodGenerator.returnValue();
        methodGenerator.endMethod();
    }

    @Override
    public void generateConstructor(final GeneratorAdapterFactory factory, final MethodCallData modified, final ConstructorCallData original) {
        throw new UnsupportedOperationException("Doesn't work with constructors");
    }

    public record Versioned(Set<ClassDesc> owners, ClassDesc newOwner, String newMethodName, VersionedMatcher<MethodMatcher> versions) implements VersionedRuleFactory {

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            return this.versions.ruleForVersion(apiVersion, match -> new MoveInstanceMethod(this.owners(), match, this.newOwner(), this.newMethodName()));
        }
    }
}
