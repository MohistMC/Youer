package io.papermc.asm;

import io.papermc.asm.rules.RewriteRule;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.objectweb.asm.ClassVisitor;

@DefaultQualifier(NonNull.class)
public interface RewriteRuleVisitorFactory {
    ClassVisitor createVisitor(ClassVisitor parent);

    static RewriteRuleVisitorFactory create(
        final int api,
        final Supplier<RewriteRule> ruleFactory,
        final io.papermc.asm.ClassInfoProvider classInfoProvider
    ) {
        return new AbstractRewriteRuleVisitorFactory(api, classInfoProvider) {
            @Override
            protected RewriteRule createRule() {
                return ruleFactory.get();
            }
        };
    }

    static RewriteRuleVisitorFactory create(
        final int api,
        final RewriteRule rule,
        final io.papermc.asm.ClassInfoProvider classInfoProvider
    ) {
        return create(api, () -> rule, classInfoProvider);
    }

    static RewriteRuleVisitorFactory create(
        final int api,
        final Consumer<RewriteRule.ChainBuilder> builderConsumer,
        final ClassInfoProvider classInfoProvider
    ) {
        return create(api, () -> {
            final RewriteRule.ChainBuilder builder = RewriteRule.chain();
            builderConsumer.accept(builder);
            return builder.build();
        }, classInfoProvider);
    }
}
