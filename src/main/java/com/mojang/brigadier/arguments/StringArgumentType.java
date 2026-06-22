package com.mojang.brigadier.arguments;

import java.util.Arrays;
import java.util.Collection;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;

public class StringArgumentType implements ArgumentType<String>
{
    private final StringType type;
    
    private StringArgumentType(final StringType type) {
        this.type = type;
    }
    
    public static StringArgumentType word() {
        return new StringArgumentType(StringType.SINGLE_WORD);
    }
    
    public static StringArgumentType string() {
        return new StringArgumentType(StringType.QUOTABLE_PHRASE);
    }
    
    public static StringArgumentType greedyString() {
        return new StringArgumentType(StringType.GREEDY_PHRASE);
    }
    
    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }
    
    public StringType getType() {
        return this.type;
    }
    
    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        if (this.type == StringType.GREEDY_PHRASE) {
            final String text = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());
            return text;
        }
        if (this.type == StringType.SINGLE_WORD) {
            return reader.readUnquotedString();
        }
        return reader.readString();
    }
    
    @Override
    public String toString() {
        return "string()";
    }
    
    @Override
    public Collection<String> getExamples() {
        return this.type.getExamples();
    }
    
    public static String escapeIfRequired(final String input) {
        for (final char c : input.toCharArray()) {
            if (!StringReader.isAllowedInUnquotedString(c)) {
                return escape(input);
            }
        }
        return input;
    }
    
    private static String escape(final String input) {
        final StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < input.length(); ++i) {
            final char c = input.charAt(i);
            if (c == '\\' || c == '\"') {
                result.append('\\');
            }
            result.append(c);
        }
        result.append("\"");
        return result.toString();
    }
    
    public enum StringType
    {
        SINGLE_WORD(new String[] { "word", "words_with_underscores" }), 
        QUOTABLE_PHRASE(new String[] { "\"quoted phrase\"", "word", "\"\"" }), 
        GREEDY_PHRASE(new String[] { "word", "words with spaces", "\"and symbols\"" });
        
        private final Collection<String> examples;
        
        private StringType(final String[] examples) {
            this.examples = Arrays.asList(examples);
        }
        
        public Collection<String> getExamples() {
            return this.examples;
        }
    }
}
