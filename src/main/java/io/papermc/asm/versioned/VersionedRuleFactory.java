package io.papermc.asm.versioned;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.util.DescriptorUtils;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A factory for {@link RewriteRule} that are determined
 * by a {@link io.papermc.asm.versioned.ApiVersion}.
 */
public interface VersionedRuleFactory {

    VersionedRuleFactory EMPTY = apiVersion -> RewriteRule.EMPTY;

    @SafeVarargs
    static VersionedRuleFactory forOwnerClass(final Class<?> owner, final Consumer<? super OwnedVersionedRuleFactoryFactory> firstFactoryConsumer, final Consumer<? super OwnedVersionedRuleFactoryFactory>... factoryConsumers) {
        return forOwnerClasses(Collections.singleton(owner), firstFactoryConsumer, factoryConsumers);
    }

    @SafeVarargs
    static VersionedRuleFactory forOwnerClasses(final Set<Class<?>> owners, final Consumer<? super OwnedVersionedRuleFactoryFactory> firstFactoryConsumer, final Consumer<? super OwnedVersionedRuleFactoryFactory>... factoryConsumers) {
        return forOwners(owners.stream().map(DescriptorUtils::desc).collect(Collectors.toUnmodifiableSet()), firstFactoryConsumer, factoryConsumers);
    }

    @SafeVarargs
    static VersionedRuleFactory forOwner(final ClassDesc owner, final Consumer<? super OwnedVersionedRuleFactoryFactory> firstFactoryConsumer, final Consumer<? super OwnedVersionedRuleFactoryFactory>... factoryConsumers) {
        return forOwners(Collections.singleton(owner), firstFactoryConsumer, factoryConsumers);
    }

    @SafeVarargs
    static VersionedRuleFactory forOwners(final Set<ClassDesc> owners, final Consumer<? super OwnedVersionedRuleFactoryFactory> firstFactoryConsumer, final Consumer<? super OwnedVersionedRuleFactoryFactory>... factoryConsumers) {
        final OwnedVersionedRuleFactoryFactory factory = OwnedVersionedRuleFactoryFactory.create(owners);
        firstFactoryConsumer.accept(factory);
        for (final Consumer<? super OwnedVersionedRuleFactoryFactory> factoryConsumer : factoryConsumers) {
            factoryConsumer.accept(factory);
        }
        return factory.build();
    }

    static VersionedRuleFactory chain(final VersionedRuleFactory... factories) {
        return chain(Arrays.asList(factories));
    }

    static VersionedRuleFactory chain(final Collection<? extends VersionedRuleFactory> factories) {
        if (factories.isEmpty()) {
            return EMPTY;
        } else if (factories.size() == 1) {
            return factories.iterator().next();
        }
        return new Chain(List.copyOf(factories));
    }

    RewriteRule createRule(io.papermc.asm.versioned.ApiVersion<?> apiVersion);

    record Chain(List<VersionedRuleFactory> factories) implements VersionedRuleFactory {

        public Chain {
            factories = List.copyOf(factories);
        }

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            final List<RewriteRule> rules = new ArrayList<>();
            for (final VersionedRuleFactory factory : this.factories) {
                final @Nullable RewriteRule rule = factory.createRule(apiVersion);
                if (rule != RewriteRule.EMPTY) {
                    rules.add(rule);
                }
            }
            return RewriteRule.chain(rules);
        }
    }
}
