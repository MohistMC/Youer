package com.mojang.brigadier.builder;

import java.util.Iterator;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

public class LiteralArgumentBuilder<S> extends ArgumentBuilder<S, LiteralArgumentBuilder<S>>
{
    private final String literal;
    
    protected LiteralArgumentBuilder(final String literal) {
        this.literal = literal;
    }
    
    public static <S> LiteralArgumentBuilder<S> literal(final String name) {
        return new LiteralArgumentBuilder<S>(name);
    }
    
    @Override
    protected LiteralArgumentBuilder<S> getThis() {
        return this;
    }
    
    public String getLiteral() {
        return this.literal;
    }
    
    @Override
    public LiteralCommandNode<S> build() {
        final LiteralCommandNode<S> result = new LiteralCommandNode<S>(this.getLiteral(), this.getCommand(), this.getRequirement(), this.getRedirect(), this.getRedirectModifier(), this.isFork());
        for (final CommandNode<S> argument : this.getArguments()) {
            result.addChild(argument);
        }
        return result;
    }
}
