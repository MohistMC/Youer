package com.mojang.brigadier;

import java.util.Collection;
import com.mojang.brigadier.tree.CommandNode;

@FunctionalInterface
public interface AmbiguityConsumer<S>
{
    void ambiguous(final CommandNode<S> p0, final CommandNode<S> p1, final CommandNode<S> p2, final Collection<String> p3);
}
