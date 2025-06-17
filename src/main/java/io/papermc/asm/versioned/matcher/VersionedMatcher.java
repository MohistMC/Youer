package io.papermc.asm.versioned.matcher;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcher;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import io.papermc.asm.rules.builder.matcher.method.targeted.TargetedMethodMatcher;
import io.papermc.asm.versioned.ApiVersion;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VersionedMatcher<C> {

    public static <C> VersionedMatcher<C> single(final ApiVersion<?> apiVersion, final C context) {
        return new VersionedMatcher<>(new TreeMap<>(Map.of(apiVersion, context)));
    }

    public static VersionedMatcherBuilder<FieldMatcher> fieldBuilder() {
        return builder();
    }

    public static VersionedMatcherBuilder<MethodMatcher> methodBuilder() {
        return builder();
    }

    public static VersionedMatcherBuilder<TargetedMethodMatcher> targetedMethodBuilder() {
        return builder();
    }

    public static <C> VersionedMatcherBuilder<C> builder() {
        return new VersionedMatcherBuilderImpl<>();
    }

    private final NavigableMap<ApiVersion<?>, C> map;

    public VersionedMatcher(final NavigableMap<ApiVersion<?>, C> map) {
        this.map = map;
    }

    public RewriteRule ruleForVersion(final ApiVersion<?> version, final Function<C, ? extends RewriteRule> creator) {
        return ruleForVersion(this.map, version, creator);
    }

    public static <P> RewriteRule ruleForVersion(final NavigableMap<ApiVersion<?>, P> versions, final ApiVersion<?> version, final Function<P, ? extends RewriteRule> creator) {
        final Map.@Nullable Entry<ApiVersion<?>, P> entry = versions.ceilingEntry(version);
        if (entry == null) {
            return RewriteRule.EMPTY;
        }
        return creator.apply(entry.getValue());
    }
}
