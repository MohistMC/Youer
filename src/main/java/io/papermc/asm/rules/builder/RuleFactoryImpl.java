package io.papermc.asm.rules.builder;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcher;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcherBuilder;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.rules.field.FieldToMethodRewrite;
import io.papermc.asm.rules.method.DirectStaticRewrite;
import io.papermc.asm.rules.method.MoveInstanceMethod;
import io.papermc.asm.rules.method.params.DirectParameterRewrite;
import io.papermc.asm.rules.method.params.FuzzyParameterRewrite;
import io.papermc.asm.rules.method.params.SuperTypeParamRewrite;
import io.papermc.asm.rules.method.returns.DirectReturnRewrite;
import io.papermc.asm.rules.method.returns.SubTypeReturnRewrite;
import io.papermc.asm.util.Builder;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

class RuleFactoryImpl implements RuleFactory {

    final Set<ClassDesc> owners;
    final List<RewriteRule> rules = new ArrayList<>();

    RuleFactoryImpl(final Set<ClassDesc> owners) {
        this.owners = Set.copyOf(owners);
    }

    private static <M, B extends Builder<M>> M build(final Consumer<? super B> builderConsumer, final Supplier<B> supplier) {
        final B builder = supplier.get();
        builderConsumer.accept(builder);
        return builder.build();
    }

    @Override
    public void plainStaticRewrite(final ClassDesc newOwner, final MethodMatcher methodMatcher) {
        this.addRule(new DirectStaticRewrite(this.owners, methodMatcher, newOwner));
    }

    @Override
    public void plainStaticRewrite(final ClassDesc newOwner, final MethodMatcher methodMatcher, final String staticMethodName) {
        this.addRule(new DirectStaticRewrite(this.owners, staticMethodName, methodMatcher, newOwner));
    }

    @Override
    public void changeParamToSuper(final ClassDesc newParamType, final TargetedMethodMatcher methodMatcher) {
        this.addRule(new SuperTypeParamRewrite(this.owners, methodMatcher, newParamType));
    }

    @Override
    public void changeParamFuzzy(final ClassDesc newParamType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.addRule(new FuzzyParameterRewrite(this.owners, newParamType, targetedMethodMatcher, isStatic(staticHandler)));
    }

    @Override
    public void changeParamDirect(final ClassDesc newParamType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.addRule(new DirectParameterRewrite(this.owners, newParamType, targetedMethodMatcher, isStatic(staticHandler)));
    }

    @Override
    public void changeReturnTypeToSub(final ClassDesc newReturnType, final TargetedMethodMatcher methodMatcher) {
        this.addRule(new SubTypeReturnRewrite(this.owners, methodMatcher, newReturnType));
    }

    @Override
    public void changeReturnTypeDirect(final ClassDesc newReturnType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeReturnTypeDirect(newReturnType, staticHandler, targetedMethodMatcher, false);
    }

    @Override
    public void changeReturnTypeDirectWithContext(final ClassDesc newReturnType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeReturnTypeDirect(newReturnType, staticHandler, targetedMethodMatcher, true);
    }

    private void changeReturnTypeDirect(final ClassDesc newReturnType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher, final boolean includeOwnerContext) {
        this.addRule(new DirectReturnRewrite(this.owners, newReturnType, targetedMethodMatcher, isStatic(staticHandler), includeOwnerContext));
    }

    @Override
    public void changeFieldToMethod(final @Nullable String getterName, final @Nullable String setterName, final boolean isInterfaceMethod, final Consumer<? super FieldMatcherBuilder> builderConsumer) {
        this.addRule(new FieldToMethodRewrite(this.owners, build(builderConsumer, FieldMatcher::builder), getterName, setterName, isInterfaceMethod));
    }

    @Override
    public void moveInstanceMethod(final ClassDesc newOwner, final String newMethodName, final MethodMatcher methodMatcher) {
        this.addRule(new MoveInstanceMethod(this.owners, methodMatcher, newOwner, newMethodName));
    }

    @Override
    public void addRule(final RewriteRule rule) {
        this.rules.add(rule);
    }

    private static Method isStatic(final Method staticHandler) {
        if (!Modifier.isStatic(staticHandler.getModifiers())) {
            throw new IllegalArgumentException(staticHandler + " isn't a static method");
        }
        return staticHandler;
    }

    @Override
    public RewriteRule build() {
        return RewriteRule.chain(this.rules);
    }
}
