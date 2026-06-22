package com.mojang.brigadier.tree;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.Collection;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.RedirectModifier;
import java.util.function.Predicate;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.ArgumentType;

public class ArgumentCommandNode<S, T> extends CommandNode<S>
{
    private static final String USAGE_ARGUMENT_OPEN = "<";
    private static final String USAGE_ARGUMENT_CLOSE = ">";
    private final String name;
    private final ArgumentType<T> type;
    private final SuggestionProvider<S> customSuggestions;
    
    public ArgumentCommandNode(final String name, final ArgumentType<T> type, final Command<S> command, final Predicate<S> requirement, final CommandNode<S> redirect, final RedirectModifier<S> modifier, final boolean forks, final SuggestionProvider<S> customSuggestions) {
        super(command, requirement, redirect, modifier, forks);
        this.name = name;
        this.type = type;
        this.customSuggestions = customSuggestions;
    }
    
    public ArgumentType<T> getType() {
        return this.type;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getUsageText() {
        return "<" + this.name + ">";
    }
    
    public SuggestionProvider<S> getCustomSuggestions() {
        return this.customSuggestions;
    }
    
    @Override
    public void parse(final StringReader reader, final CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final T result = this.type.parse(reader, contextBuilder.getSource());
        final ParsedArgument<S, T> parsed = new ParsedArgument<S, T>(start, reader.getCursor(), result);
        contextBuilder.withArgument(this.name, parsed);
        contextBuilder.withNode(this, parsed.getRange());
    }
    
    @Override
    public CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) throws CommandSyntaxException {
        if (this.customSuggestions == null) {
            return this.type.listSuggestions(context, builder);
        }
        return this.customSuggestions.getSuggestions(context, builder);
    }
    
    @Override
    public RequiredArgumentBuilder<S, T> createBuilder() {
        final RequiredArgumentBuilder<S, T> builder = RequiredArgumentBuilder.argument(this.name, this.type);
        builder.requires(this.getRequirement());
        builder.forward(this.getRedirect(), this.getRedirectModifier(), this.isFork());
        builder.suggests(this.customSuggestions);
        if (this.getCommand() != null) {
            builder.executes(this.getCommand());
        }
        return builder;
    }
    
    public boolean isValidInput(final String input) {
        try {
            final StringReader reader = new StringReader(input);
            this.type.parse(reader);
            return !reader.canRead() || reader.peek() == ' ';
        }
        catch (final CommandSyntaxException ignored) {
            return false;
        }
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArgumentCommandNode)) {
            return false;
        }
        final ArgumentCommandNode that = (ArgumentCommandNode)o;
        return this.name.equals(that.name) && this.type.equals(that.type) && super.equals(o);
    }
    
    @Override
    public int hashCode() {
        int result = this.name.hashCode();
        result = 31 * result + this.type.hashCode();
        return result;
    }
    
    @Override
    protected String getSortedKey() {
        return this.name;
    }
    
    @Override
    public Collection<String> getExamples() {
        return this.type.getExamples();
    }
    
    @Override
    public String toString() {
        return "<argument " + this.name + ":" + this.type + ">";
    }
}
