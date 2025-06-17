package io.papermc.asm.rules.rename;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.rename.asm.FixedClassRemapper;
import io.papermc.asm.versioned.Mergeable;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

public final class RenameRule implements RewriteRule.Delegate, Mergeable<RenameRule> {

    public static RenameRuleBuilder builder() {
        return new RenameRuleBuilderImpl();
    }

    private final Map<String, String> renames;
    private final Map<ClassDesc, EnumRenamer> enumFieldRenames;
    private @MonotonicNonNull RewriteRule rule;

    public RenameRule(final Map<String, String> renames, final Map<ClassDesc, EnumRenamer> enumFieldRenames) {
        this.renames = Map.copyOf(renames);
        this.enumFieldRenames = Map.copyOf(enumFieldRenames);
    }

    public Map<String, String> renames() {
        return this.renames;
    }

    public Map<ClassDesc, EnumRenamer> enumFieldRenames() {
        return this.enumFieldRenames;
    }

    @Override
    public RewriteRule delegate() {
        if (this.rule == null) {
            final Remapper remapper = new SimpleRemapper(Map.copyOf(this.renames));

            final List<RewriteRule> rules = new ArrayList<>(this.enumFieldRenames.size() + 1);
            this.enumFieldRenames.forEach((classDesc, enumRenamer) -> {
                rules.add(new EnumValueOfRewriteRule(enumRenamer));
            });
            rules.add((api, parent, context) -> new FixedClassRemapper(api, parent, remapper));

            this.rule = RewriteRule.chain(rules);
        }
        return this.rule;
    }

    @Override
    public RenameRule merge(final RenameRule other) {
        final Map<String, String> regularRenames = new HashMap<>(this.renames);
        final Map<ClassDesc, EnumRenamer> enumFieldRenames = new HashMap<>(this.enumFieldRenames);
        regularRenames.putAll(other.renames);
        other.enumFieldRenames.forEach((cd, renamer) -> {
            enumFieldRenames.merge(cd, renamer, EnumRenamer::overwrite);
        });
        return new RenameRule(regularRenames, enumFieldRenames);
    }
}
