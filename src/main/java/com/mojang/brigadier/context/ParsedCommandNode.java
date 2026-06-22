package com.mojang.brigadier.context;

import java.util.Objects;
import com.mojang.brigadier.tree.CommandNode;

public class ParsedCommandNode<S>
{
    private final CommandNode<S> node;
    private final StringRange range;
    
    public ParsedCommandNode(final CommandNode<S> node, final StringRange range) {
        this.node = node;
        this.range = range;
    }
    
    public CommandNode<S> getNode() {
        return this.node;
    }
    
    public StringRange getRange() {
        return this.range;
    }
    
    @Override
    public String toString() {
        return this.node + "@" + this.range;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final ParsedCommandNode<?> that = (ParsedCommandNode<?>)o;
        return Objects.equals(this.node, that.node) && Objects.equals(this.range, that.range);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.node, this.range);
    }
}
