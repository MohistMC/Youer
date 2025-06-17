package io.papermc.asm.versioned;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcher;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Method;
import java.util.NavigableMap;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.papermc.asm.util.DescriptorUtils.desc;

public interface OwnedVersionedRuleFactoryFactory {

    static OwnedVersionedRuleFactoryFactory create(final Set<ClassDesc> owners) {
        return new OwnedVersionedRuleFactoryFactoryImpl(owners);
    }

    //<editor-fold desc="changeParamToSuper" defaultstate="collapsed">
    default void plainStaticRewrite(final ClassDesc newOwner, final @Nullable String staticMethodName, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final MethodMatcher matcher) {
        this.plainStaticRewrite(newOwner, staticMethodName, VersionedMatcher.single(apiVersion, matcher));
    }

    void plainStaticRewrite(ClassDesc newOwner, @Nullable String staticMethodName, VersionedMatcher<MethodMatcher> versions);
    //</editor-fold>

    //<editor-fold desc="changeParamToSuper" defaultstate="collapsed">
    default void changeParamToSuper(final Class<?> newParamType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final TargetedMethodMatcher methodMatcher) {
        this.changeParamToSuper(desc(newParamType), apiVersion, methodMatcher);
    }

    default void changeParamToSuper(final ClassDesc newParamType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final TargetedMethodMatcher methodMatcher) {
        this.changeParamToSuper(newParamType, VersionedMatcher.single(apiVersion, methodMatcher));
    }

    default void changeParamToSuper(final Class<?> newParamType, final VersionedMatcher<TargetedMethodMatcher> versions) {
        this.changeParamToSuper(desc(newParamType), versions);
    }

    void changeParamToSuper(ClassDesc newParamType, VersionedMatcher<TargetedMethodMatcher> versions);
    //</editor-fold>

    //<editor-fold desc="changeParamFuzzy" defaultstate="collapsed">
    default void changeParamFuzzy(final Class<?> newParamType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeParamFuzzy(desc(newParamType), apiVersion, staticHandler, targetedMethodMatcher);
    }

    default void changeParamFuzzy(final ClassDesc newParamType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher targetedMethodMatcher) {
        this.changeParamFuzzy(newParamType, VersionedMatcher.single(apiVersion, new TargetedMethodMatcherWithHandler(targetedMethodMatcher, staticHandler)));
    }

    default void changeParamFuzzy(final Class<?> newParamType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.changeParamFuzzy(desc(newParamType), versions);
    }

    void changeParamFuzzy(ClassDesc newParamType, VersionedMatcher<TargetedMethodMatcherWithHandler> versions);
    //</editor-fold>

    //<editor-fold desc="changeParamDirect" defaultstate="collapsed">
    default void changeParamDirect(final Class<?> newParamType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher methodMatcher) {
        this.changeParamDirect(desc(newParamType), apiVersion, staticHandler, methodMatcher);
    }

    default void changeParamDirect(final ClassDesc newParamType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher methodMatcher) {
        this.changeParamDirect(newParamType, VersionedMatcher.single(apiVersion, new TargetedMethodMatcherWithHandler(methodMatcher, staticHandler)));
    }

    default void changeParamDirect(final Class<?> newParamType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.changeParamDirect(desc(newParamType), versions);
    }

    void changeParamDirect(ClassDesc newParamType, VersionedMatcher<TargetedMethodMatcherWithHandler> versions);
    //</editor-fold>

    //<editor-fold desc="changeReturnTypeToSub" defaultstate="collapsed">
    default void changeReturnTypeToSub(final Class<?> newReturnType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeToSub(desc(newReturnType), apiVersion, methodMatcher);
    }

    default void changeReturnTypeToSub(final ClassDesc newReturnType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeToSub(newReturnType, VersionedMatcher.single(apiVersion, methodMatcher));
    }

    default void changeReturnTypeToSub(final Class<?> newReturnType, final VersionedMatcher<TargetedMethodMatcher> versions) {
        this.changeReturnTypeToSub(desc(newReturnType), versions);
    }

    void changeReturnTypeToSub(ClassDesc newReturnType, VersionedMatcher<TargetedMethodMatcher> versions);
    //</editor-fold>

    //<editor-fold desc="changeReturnTypeDirect" defaultstate="collapsed">
    default void changeReturnTypeDirect(final Class<?> newReturnType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeDirect(desc(newReturnType), apiVersion, staticHandler, methodMatcher);
    }

    default void changeReturnTypeDirect(final ClassDesc newReturnType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeDirect(newReturnType, VersionedMatcher.single(apiVersion, new TargetedMethodMatcherWithHandler(methodMatcher, staticHandler)));
    }

    default void changeReturnTypeDirect(final Class<?> newReturnType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.changeReturnTypeDirect(desc(newReturnType), versions);
    }

    void changeReturnTypeDirect(ClassDesc newReturnType, VersionedMatcher<TargetedMethodMatcherWithHandler> versions);
    //</editor-fold>

    //<editor-fold desc="changeReturnTypeDirectWithContext" defaultstate="collapsed">
    default void changeReturnTypeDirectWithContext(final Class<?> newReturnType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeDirectWithContext(desc(newReturnType), apiVersion, staticHandler, methodMatcher);
    }

    default void changeReturnTypeDirectWithContext(final ClassDesc newReturnType, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final Method staticHandler, final TargetedMethodMatcher methodMatcher) {
        this.changeReturnTypeDirectWithContext(newReturnType, VersionedMatcher.single(apiVersion, new TargetedMethodMatcherWithHandler(methodMatcher, staticHandler)));
    }

    default void changeReturnTypeDirectWithContext(final Class<?> newReturnType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.changeReturnTypeDirectWithContext(desc(newReturnType), versions);
    }

    void changeReturnTypeDirectWithContext(ClassDesc newReturnType, VersionedMatcher<TargetedMethodMatcherWithHandler> versions);
    //</editor-fold>

    //<editor-fold desc="changeFieldToMethod" defaultstate="collapsed">
    default void changeFieldToMethod(final @Nullable String getterName, final @Nullable String setterName, final boolean isInterfaceMethod, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final FieldMatcher matcher) {
        this.changeFieldToMethod(getterName, setterName, isInterfaceMethod, VersionedMatcher.single(apiVersion, matcher));
    }

    void changeFieldToMethod(@Nullable String getterName, @Nullable String setterName, boolean isInterfaceMethod, VersionedMatcher<FieldMatcher> versions);
    //</editor-fold>

    //<editor-fold desc="moveInstanceMethod" defaultstate="collapsed">
    default void moveInstanceMethod(final Class<?> newOwner, final String newMethodName, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final MethodMatcher matcher) {
        this.moveInstanceMethod(desc(newOwner), newMethodName, apiVersion, matcher);
    }

    default void moveInstanceMethod(final ClassDesc newOwner, final String newMethodName, final io.papermc.asm.versioned.ApiVersion<?> apiVersion, final MethodMatcher matcher) {
        this.moveInstanceMethod(newOwner, newMethodName, VersionedMatcher.single(apiVersion, matcher));
    }

    default void moveInstanceMethod(final Class<?> newOwner, final String newMethodName, final VersionedMatcher<MethodMatcher> versions) {
        this.moveInstanceMethod(desc(newOwner), newMethodName, versions);
    }

    void moveInstanceMethod(ClassDesc newOwner, String newMethodName, VersionedMatcher<MethodMatcher> versions);
    //</editor-fold>

    <R extends RewriteRule & Mergeable<R>> void addMergeableRuleFactory(NavigableMap<io.papermc.asm.versioned.ApiVersion<?>, R> versions);

    void addChainableRuleFactory(NavigableMap<ApiVersion<?>, ? extends RewriteRule> versions);

    void addRuleFactory(io.papermc.asm.versioned.VersionedRuleFactory factory);

    VersionedRuleFactory build();
}
