package com.mojang.brigadier.arguments;

import java.util.Arrays;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;

public class BoolArgumentType implements ArgumentType<Boolean>
{
    private static final Collection<String> EXAMPLES;
    
    private BoolArgumentType() {
    }
    
    public static BoolArgumentType bool() {
        return new BoolArgumentType();
    }
    
    public static boolean getBool(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Boolean.class);
    }
    
    @Override
    public Boolean parse(final StringReader reader) throws CommandSyntaxException {
        return reader.readBoolean();
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        if ("true".startsWith(builder.getRemainingLowerCase())) {
            builder.suggest("true");
        }
        if ("false".startsWith(builder.getRemainingLowerCase())) {
            builder.suggest("false");
        }
        return builder.buildFuture();
    }
    
    @Override
    public Collection<String> getExamples() {
        return BoolArgumentType.EXAMPLES;
    }
    
    static {
        EXAMPLES = Arrays.asList("true", "false");
    }
}
