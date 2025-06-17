package io.papermc.asm.rules.builder;

import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import java.lang.constant.ClassDesc;
import java.util.Set;
import java.util.function.Consumer;

public interface ConfiguredRuleFactory extends RuleFactory {

    static ConfiguredRuleFactory create(final Set<ClassDesc> owners, final RuleFactoryConfiguration config) {
        return new ConfiguredRuleFactoryImpl(owners, config);
    }

    static Factory combine(final Factory... factories) {
        return r -> {
            for (final Factory factory : factories) {
                factory.accept(r);
            }
        };
    }

    void plainStaticRewrite(MethodMatcher methodMatcher);

    @FunctionalInterface
    interface Factory extends Consumer<ConfiguredRuleFactory> {

        @Override
        void accept(ConfiguredRuleFactory factory);
    }
}
