package io.papermc.asm.rules;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.util.DescriptorUtils;
import java.lang.constant.ClassDesc;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Represents a {@link io.papermc.asm.rules.RewriteRule} that has a set
 * of specific owners that it targets. The owners can be
 * for a targeted method or a field.
 */
public interface OwnableRewriteRule extends RewriteRule {

    Set<ClassDesc> owners();

    default boolean matchesOwner(final ClassProcessingContext context, final String owner) {
        return this.owners().stream().map(DescriptorUtils::toOwner).anyMatch(Predicate.isEqual(owner));
    }
}
