package com.mojang.brigadier.context;

import java.util.Iterator;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.Collection;
import com.mojang.brigadier.ResultConsumer;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

public class ContextChain<S>
{
    private final List<CommandContext<S>> modifiers;
    private final CommandContext<S> executable;
    private ContextChain<S> nextStageCache;
    
    public ContextChain(final List<CommandContext<S>> modifiers, final CommandContext<S> executable) {
        this.nextStageCache = null;
        if (executable.getCommand() == null) {
            throw new IllegalArgumentException("Last command in chain must be executable");
        }
        this.modifiers = modifiers;
        this.executable = executable;
    }
    
    public static <S> Optional<ContextChain<S>> tryFlatten(final CommandContext<S> rootContext) {
        final List<CommandContext<S>> modifiers = new ArrayList<CommandContext<S>>();
        CommandContext<S> current = rootContext;
        while (true) {
            final CommandContext<S> child = current.getChild();
            if (child == null) {
                break;
            }
            modifiers.add(current);
            current = child;
        }
        if (current.getCommand() == null) {
            return Optional.empty();
        }
        return Optional.of(new ContextChain<S>(modifiers, current));
    }
    
    public static <S> Collection<S> runModifier(final CommandContext<S> modifier, final S source, final ResultConsumer<S> resultConsumer, final boolean forkedMode) throws CommandSyntaxException {
        final RedirectModifier<S> sourceModifier = modifier.getRedirectModifier();
        if (sourceModifier == null) {
            return Collections.singleton(source);
        }
        final CommandContext<S> contextToUse = modifier.copyFor(source);
        try {
            return sourceModifier.apply(contextToUse);
        }
        catch (final CommandSyntaxException ex) {
            resultConsumer.onCommandComplete(contextToUse, false, 0);
            if (forkedMode) {
                return (Collection<S>)Collections.emptyList();
            }
            throw ex;
        }
    }
    
    public static <S> int runExecutable(final CommandContext<S> executable, final S source, final ResultConsumer<S> resultConsumer, final boolean forkedMode) throws CommandSyntaxException {
        final CommandContext<S> contextToUse = executable.copyFor(source);
        try {
            final int result = executable.getCommand().run(contextToUse);
            resultConsumer.onCommandComplete(contextToUse, true, result);
            return forkedMode ? 1 : result;
        }
        catch (final CommandSyntaxException ex) {
            resultConsumer.onCommandComplete(contextToUse, false, 0);
            if (forkedMode) {
                return 0;
            }
            throw ex;
        }
    }
    
    public int executeAll(final S source, final ResultConsumer<S> resultConsumer) throws CommandSyntaxException {
        if (this.modifiers.isEmpty()) {
            return runExecutable(this.executable, source, resultConsumer, false);
        }
        boolean forkedMode = false;
        List<S> currentSources = Collections.singletonList(source);
        for (final CommandContext<S> modifier : this.modifiers) {
            forkedMode |= modifier.isForked();
            final List<S> nextSources = new ArrayList<S>();
            for (final S sourceToRun : currentSources) {
                nextSources.addAll(runModifier(modifier, (S)sourceToRun, resultConsumer, forkedMode));
            }
            if (nextSources.isEmpty()) {
                return 0;
            }
            currentSources = nextSources;
        }
        int result = 0;
        for (final S executionSource : currentSources) {
            result += runExecutable(this.executable, executionSource, resultConsumer, forkedMode);
        }
        return result;
    }
    
    public Stage getStage() {
        return this.modifiers.isEmpty() ? Stage.EXECUTE : Stage.MODIFY;
    }
    
    public CommandContext<S> getTopContext() {
        if (this.modifiers.isEmpty()) {
            return this.executable;
        }
        return this.modifiers.get(0);
    }
    
    public ContextChain<S> nextStage() {
        final int modifierCount = this.modifiers.size();
        if (modifierCount == 0) {
            return null;
        }
        if (this.nextStageCache == null) {
            this.nextStageCache = new ContextChain<S>(this.modifiers.subList(1, modifierCount), this.executable);
        }
        return this.nextStageCache;
    }
    
    public enum Stage
    {
        MODIFY, 
        EXECUTE;
    }
}
