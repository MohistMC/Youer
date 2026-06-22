package com.mojang.brigadier.context;

import java.util.HashMap;
import com.mojang.brigadier.RedirectModifier;
import java.util.List;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.Command;
import java.util.Map;

public class CommandContext<S>
{
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;
    private final S source;
    private final String input;
    private final Command<S> command;
    private final Map<String, ParsedArgument<S, ?>> arguments;
    private final CommandNode<S> rootNode;
    private final List<ParsedCommandNode<S>> nodes;
    private final StringRange range;
    private final CommandContext<S> child;
    private final RedirectModifier<S> modifier;
    private final boolean forks;
    
    public CommandContext(final S source, final String input, final Map<String, ParsedArgument<S, ?>> arguments, final Command<S> command, final CommandNode<S> rootNode, final List<ParsedCommandNode<S>> nodes, final StringRange range, final CommandContext<S> child, final RedirectModifier<S> modifier, final boolean forks) {
        this.source = source;
        this.input = input;
        this.arguments = arguments;
        this.command = command;
        this.rootNode = rootNode;
        this.nodes = nodes;
        this.range = range;
        this.child = child;
        this.modifier = modifier;
        this.forks = forks;
    }
    
    public CommandContext<S> copyFor(final S source) {
        if (this.source == source) {
            return this;
        }
        return new CommandContext<S>(source, this.input, this.arguments, this.command, this.rootNode, this.nodes, this.range, this.child, this.modifier, this.forks);
    }
    
    public CommandContext<S> getChild() {
        return this.child;
    }
    
    public CommandContext<S> getLastChild() {
        CommandContext<S> result;
        for (result = this; result.getChild() != null; result = result.getChild()) {}
        return result;
    }
    
    public Command<S> getCommand() {
        return this.command;
    }
    
    public S getSource() {
        return this.source;
    }
    
    public <V> V getArgument(final String name, final Class<V> clazz) {
        final ParsedArgument<S, ?> argument = this.arguments.get(name);
        if (argument == null) {
            throw new IllegalArgumentException("No such argument '" + name + "' exists on this command");
        }
        final Object result = argument.getResult();
        if (CommandContext.PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(result.getClass())) {
            return (V)result;
        }
        throw new IllegalArgumentException("Argument '" + name + "' is defined as " + result.getClass().getSimpleName() + ", not " + clazz);
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommandContext)) {
            return false;
        }
        final CommandContext that = (CommandContext)o;
        if (!this.arguments.equals(that.arguments)) {
            return false;
        }
        if (!this.rootNode.equals(that.rootNode)) {
            return false;
        }
        if (this.nodes.size() != that.nodes.size() || !this.nodes.equals(that.nodes)) {
            return false;
        }
        Label_0127: {
            if (this.command != null) {
                if (this.command.equals(that.command)) {
                    break Label_0127;
                }
            }
            else if (that.command == null) {
                break Label_0127;
            }
            return false;
        }
        if (!this.source.equals(that.source)) {
            return false;
        }
        if (this.child != null) {
            if (this.child.equals(that.child)) {
                return true;
            }
        }
        else if (that.child == null) {
            return true;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int result = this.source.hashCode();
        result = 31 * result + this.arguments.hashCode();
        result = 31 * result + ((this.command != null) ? this.command.hashCode() : 0);
        result = 31 * result + this.rootNode.hashCode();
        result = 31 * result + this.nodes.hashCode();
        result = 31 * result + ((this.child != null) ? this.child.hashCode() : 0);
        return result;
    }
    
    public RedirectModifier<S> getRedirectModifier() {
        return this.modifier;
    }
    
    public StringRange getRange() {
        return this.range;
    }
    
    public String getInput() {
        return this.input;
    }
    
    public CommandNode<S> getRootNode() {
        return this.rootNode;
    }
    
    public List<ParsedCommandNode<S>> getNodes() {
        return this.nodes;
    }
    
    public boolean hasNodes() {
        return !this.nodes.isEmpty();
    }
    
    public boolean isForked() {
        return this.forks;
    }
    
    static {
        (PRIMITIVE_TO_WRAPPER = new HashMap<Class<?>, Class<?>>()).put(Boolean.TYPE, Boolean.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Byte.TYPE, Byte.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Short.TYPE, Short.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Character.TYPE, Character.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Integer.TYPE, Integer.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Long.TYPE, Long.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Float.TYPE, Float.class);
        CommandContext.PRIMITIVE_TO_WRAPPER.put(Double.TYPE, Double.class);
    }
}
