package com.mojang.brigadier.builder;

import java.util.Iterator;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.ArgumentType;

public class RequiredArgumentBuilder<S, T> extends ArgumentBuilder<S, RequiredArgumentBuilder<S, T>>
{
    private final String name;
    private final ArgumentType<T> type;
    private SuggestionProvider<S> suggestionsProvider;
    
    private RequiredArgumentBuilder(final String name, final ArgumentType<T> type) {
        this.suggestionsProvider = null;
        this.name = name;
        this.type = type;
    }
    
    public static <S, T> RequiredArgumentBuilder<S, T> argument(final String name, final ArgumentType<T> type) {
        return new RequiredArgumentBuilder<S, T>(name, type);
    }
    
    public RequiredArgumentBuilder<S, T> suggests(final SuggestionProvider<S> provider) {
        this.suggestionsProvider = provider;
        return this.getThis();
    }
    
    public SuggestionProvider<S> getSuggestionsProvider() {
        return this.suggestionsProvider;
    }
    
    @Override
    protected RequiredArgumentBuilder<S, T> getThis() {
        return this;
    }
    
    public ArgumentType<T> getType() {
        return this.type;
    }
    
    public String getName() {
        return this.name;
    }
    
    @Override
    public ArgumentCommandNode<S, T> build() {
        final ArgumentCommandNode<S, T> result = new ArgumentCommandNode<S, T>(this.getName(), this.getType(), this.getCommand(), this.getRequirement(), this.getRedirect(), this.getRedirectModifier(), this.isFork(), this.getSuggestionsProvider());
        for (final CommandNode<S> argument : this.getArguments()) {
            result.addChild((CommandNode<S>)argument);
        }
        return result;
    }
}
