package com.mojang.brigadier;

import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface ResultConsumer<S>
{
    void onCommandComplete(final CommandContext<S> p0, final boolean p1, final int p2);
}
