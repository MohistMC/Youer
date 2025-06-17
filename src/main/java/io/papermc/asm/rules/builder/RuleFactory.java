package io.papermc.asm.rules.builder;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcherBuilder;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.papermc.asm.util.DescriptorUtils.desc;

public interface RuleFactory {

    static RuleFactory create(final Set<ClassDesc> owners) {
        return new RuleFactoryImpl(owners);
    }

    static Factory combine(final Factory... factories) {
        return r -> {
            for (final Factory factory : factories) {
                factory.accept(r);
            }
        };
    }

    void plainStaticRewrite(ClassDesc newOwner, MethodMatcher methodMatcher);

    void plainStaticRewrite(ClassDesc newOwner, MethodMatcher methodMatcher, String staticMethodName);

    default void changeParamToSuper(final Class<?> newParamType, final TargetedMethodMatcher methodMatcher) {
        this.changeParamToSuper( desc(newParamType), methodMatcher);
    }

    void changeParamToSuper(ClassDesc newParamType, TargetedMethodMatcher methodMatcher);

    default void changeParamFuzzy(final Class<?> newParamType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeParamFuzzy(desc(newParamType), staticHandler, targetedMethodMatcher);
    }

    void changeParamFuzzy(ClassDesc newParamType, Method staticHandler, TargetedMethodMatcher targetedMethodMatcher);

    default void changeParamDirect(final Class<?> newParamType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeParamDirect(desc(newParamType), staticHandler, targetedMethodMatcher);
    }

    void changeParamDirect(ClassDesc newParamType, Method staticHandler, TargetedMethodMatcher targetedMethodMatcher);

    default void changeReturnTypeToSub(final Class<?> newReturnType, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeToSub(desc(newReturnType), methodMatcher);
    }

    void changeReturnTypeToSub(ClassDesc newReturnType, TargetedMethodMatcher methodMatcher);

    default void changeReturnTypeDirect(final Class<?> newReturnType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeReturnTypeDirect(desc(newReturnType), staticHandler, targetedMethodMatcher);
    }

    void changeReturnTypeDirect(ClassDesc newReturnType, Method staticHandler, TargetedMethodMatcher targetedMethodMatcher);

    default void changeReturnTypeDirectWithContext(final Class<?> newReturnType, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeReturnTypeDirectWithContext(desc(newReturnType), staticHandler, targetedMethodMatcher);
    }

    void changeReturnTypeDirectWithContext(ClassDesc newReturnType, Method staticHandler, TargetedMethodMatcher targetedMethodMatcher);

    void changeFieldToMethod(@Nullable String getterName, @Nullable String setterName, boolean isInterfaceMethod, Consumer<? super FieldMatcherBuilder> builderConsumer);

    default void moveInstanceMethod(final Class<?> newOwner, final String newMethodName, final MethodMatcher methodMatcher) {
        this.moveInstanceMethod(desc(newOwner), newMethodName, methodMatcher);
    }

    void moveInstanceMethod(ClassDesc newOwner, String newMethodName, MethodMatcher methodMatcher);

    void addRule(RewriteRule rule);

    RewriteRule build();

    @FunctionalInterface
    interface Factory extends Consumer<RuleFactory> {

        @Override
        void accept(RuleFactory factory);
    }
}
