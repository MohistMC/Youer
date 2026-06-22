package com.mojang.brigadier;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface SingleRedirectModifier<S>
{
    S apply(final CommandContext<S> p0) throws CommandSyntaxException;
}
