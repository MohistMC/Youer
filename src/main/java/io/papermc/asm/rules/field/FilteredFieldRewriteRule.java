package io.papermc.asm.rules.field;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.OwnableRewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcher;

/**
 * A rule that targets specific fields and owners.
 */
public interface FilteredFieldRewriteRule extends FieldRewriteRule, OwnableRewriteRule {

    FieldMatcher fieldMatcher();

    @Override
    default boolean shouldProcess(final ClassProcessingContext context, final int opcode, final String owner, final String name, final String descriptor) {
        return this.matchesOwner(context, owner) && this.fieldMatcher().matches(name, descriptor);
    }
}
