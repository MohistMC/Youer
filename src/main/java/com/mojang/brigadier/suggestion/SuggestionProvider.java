package com.mojang.brigadier.suggestion;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.context.CommandContext;

@FunctionalInterface
public interface SuggestionProvider<S>
{
    CompletableFuture<Suggestions> getSuggestions(final CommandContext<S> p0, final SuggestionsBuilder p1) throws CommandSyntaxException;
}
