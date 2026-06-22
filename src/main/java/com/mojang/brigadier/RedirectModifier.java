package com.mojang.brigadier;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface RedirectModifier<S>
{
    Collection<S> apply(final CommandContext<S> p0) throws CommandSyntaxException;
}
