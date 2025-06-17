package io.papermc.asm.versioned;

import io.papermc.asm.rules.RewriteRule;

public interface Mergeable<R extends RewriteRule> {

    R merge(R other);
}
