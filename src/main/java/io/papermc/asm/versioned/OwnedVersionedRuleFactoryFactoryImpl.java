package io.papermc.asm.versioned;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcher;
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
import io.papermc.asm.versioned.matcher.TargetedMethodMatcherWithHandler;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OwnedVersionedRuleFactoryFactoryImpl implements OwnedVersionedRuleFactoryFactory {

    final Set<ClassDesc> owners;
    private final List<io.papermc.asm.versioned.VersionedRuleFactory> factories = new ArrayList<>();

    public OwnedVersionedRuleFactoryFactoryImpl(final Set<ClassDesc> owners) {
        this.owners = Set.copyOf(owners);
    }

    @Override
    public void plainStaticRewrite(final ClassDesc newOwner, final @Nullable String staticMethodName, final VersionedMatcher<MethodMatcher> versions) {
        this.factories.add(new DirectStaticRewrite.Versioned(this.owners, newOwner, staticMethodName, versions));
    }

    @Override
    public void changeParamToSuper(final ClassDesc newParamType, final VersionedMatcher<TargetedMethodMatcher> versions) {
        this.factories.add(new SuperTypeParamRewrite.Versioned(this.owners, newParamType, versions));
    }

    @Override
    public void changeParamFuzzy(final ClassDesc newParamType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.factories.add(new FuzzyParameterRewrite.Versioned(this.owners, newParamType, versions));
    }

    @Override
    public void changeParamDirect(final ClassDesc newParamType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.factories.add(new DirectParameterRewrite.Versioned(this.owners, newParamType, versions));
    }

    @Override
    public void changeReturnTypeToSub(final ClassDesc newReturnType, final VersionedMatcher<TargetedMethodMatcher> versions) {
        this.factories.add(new SubTypeReturnRewrite.Versioned(this.owners, newReturnType, versions));
    }

    @Override
    public void changeReturnTypeDirect(final ClassDesc newReturnType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.factories.add(new DirectReturnRewrite.Versioned(this.owners, newReturnType, versions, false));
    }

    @Override
    public void changeReturnTypeDirectWithContext(final ClassDesc newReturnType, final VersionedMatcher<TargetedMethodMatcherWithHandler> versions) {
        this.factories.add(new DirectReturnRewrite.Versioned(this.owners, newReturnType, versions, true));
    }

    @Override
    public void changeFieldToMethod(final @Nullable String getterName, final @Nullable String setterName, final boolean isInterfaceMethod, final VersionedMatcher<FieldMatcher> versions) {
        this.factories.add(new FieldToMethodRewrite.Versioned(this.owners, getterName, setterName, isInterfaceMethod, versions));
    }

    @Override
    public void moveInstanceMethod(final ClassDesc newOwner, final String newMethodName, final VersionedMatcher<MethodMatcher> versions) {
        this.factories.add(new MoveInstanceMethod.Versioned(this.owners, newOwner, newMethodName, versions));
    }

    @Override
    public <R extends RewriteRule & io.papermc.asm.versioned.Mergeable<R>> void addMergeableRuleFactory(final NavigableMap<io.papermc.asm.versioned.ApiVersion<?>, R> versions) {
        this.factories.add(new io.papermc.asm.versioned.MappedVersionRuleFactory<>(versions, Mergeable::merge));
    }

    @Override
    public void addChainableRuleFactory(final NavigableMap<ApiVersion<?>, ? extends RewriteRule> versions) {
        this.factories.add(new MappedVersionRuleFactory<RewriteRule>(versions, RewriteRule::chain));
    }

    @Override
    public void addRuleFactory(final io.papermc.asm.versioned.VersionedRuleFactory factory) {
        this.factories.add(factory);
    }

    @Override
    public io.papermc.asm.versioned.VersionedRuleFactory build() {
        return VersionedRuleFactory.chain(this.factories);
    }
}
