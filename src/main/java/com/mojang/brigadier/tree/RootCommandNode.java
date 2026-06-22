package com.mojang.brigadier.tree;

import java.util.Collection;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.Command;
import java.util.Collections;

public class RootCommandNode<S> extends CommandNode<S>
{
    public RootCommandNode() {
        super(null, c -> true, null, s -> Collections.singleton(s.getSource()), false);
    }
    
    @Override
    public String getName() {
        return "";
    }
    
    @Override
    public String getUsageText() {
        return "";
    }
    
    @Override
    public void parse(final StringReader reader, final CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
    }
    
    @Override
    public CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return Suggestions.empty();
    }
    
    public boolean isValidInput(final String input) {
        return false;
    }
    
    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof RootCommandNode && super.equals(o));
    }
    
    @Override
    public ArgumentBuilder<S, ?> createBuilder() {
        throw new IllegalStateException("Cannot convert root into a builder");
    }
    
    @Override
    protected String getSortedKey() {
        return "";
    }
    
    @Override
    public Collection<String> getExamples() {
        return Collections.emptyList();
    }
    
    @Override
    public String toString() {
        return "<root>";
    }
}
