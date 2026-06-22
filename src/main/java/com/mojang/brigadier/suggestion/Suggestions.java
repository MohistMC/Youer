package com.mojang.brigadier.suggestion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;
import java.util.List;
import com.mojang.brigadier.context.StringRange;

public class Suggestions
{
    private static final Suggestions EMPTY;
    private final StringRange range;
    private final List<Suggestion> suggestions;
    
    public Suggestions(final StringRange range, final List<Suggestion> suggestions) {
        this.range = range;
        this.suggestions = suggestions;
    }
    
    public StringRange getRange() {
        return this.range;
    }
    
    public List<Suggestion> getList() {
        return this.suggestions;
    }
    
    public boolean isEmpty() {
        return this.suggestions.isEmpty();
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Suggestions)) {
            return false;
        }
        final Suggestions that = (Suggestions)o;
        return Objects.equals(this.range, that.range) && Objects.equals(this.suggestions, that.suggestions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.range, this.suggestions);
    }
    
    @Override
    public String toString() {
        return "Suggestions{range=" + this.range + ", suggestions=" + this.suggestions + '}';
    }
    
    public static CompletableFuture<Suggestions> empty() {
        return CompletableFuture.completedFuture(Suggestions.EMPTY);
    }
    
    public static Suggestions merge(final String command, final Collection<Suggestions> input) {
        if (input.isEmpty()) {
            return Suggestions.EMPTY;
        }
        if (input.size() == 1) {
            return input.iterator().next();
        }
        final Set<Suggestion> texts = new HashSet<Suggestion>();
        for (final Suggestions suggestions : input) {
            texts.addAll(suggestions.getList());
        }
        return create(command, texts);
    }
    
    public static Suggestions create(final String command, final Collection<Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            return Suggestions.EMPTY;
        }
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (final Suggestion suggestion : suggestions) {
            start = Math.min(suggestion.getRange().getStart(), start);
            end = Math.max(suggestion.getRange().getEnd(), end);
        }
        final StringRange range = new StringRange(start, end);
        final Set<Suggestion> texts = new HashSet<Suggestion>();
        for (final Suggestion suggestion2 : suggestions) {
            texts.add(suggestion2.expand(command, range));
        }
        final List<Suggestion> sorted = new ArrayList<Suggestion>(texts);
        sorted.sort((a, b) -> a.compareToIgnoreCase(b));
        return new Suggestions(range, sorted);
    }
    
    static {
        EMPTY = new Suggestions(StringRange.at(0), new ArrayList<Suggestion>());
    }
}
