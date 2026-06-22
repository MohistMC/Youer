package com.mojang.brigadier.arguments;

import java.util.Arrays;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;

public class IntegerArgumentType implements ArgumentType<Integer>
{
    private static final Collection<String> EXAMPLES;
    private final int minimum;
    private final int maximum;
    
    private IntegerArgumentType(final int minimum, final int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }
    
    public static IntegerArgumentType integer() {
        return integer(Integer.MIN_VALUE);
    }
    
    public static IntegerArgumentType integer(final int min) {
        return integer(min, Integer.MAX_VALUE);
    }
    
    public static IntegerArgumentType integer(final int min, final int max) {
        return new IntegerArgumentType(min, max);
    }
    
    public static int getInteger(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Integer.TYPE);
    }
    
    public int getMinimum() {
        return this.minimum;
    }
    
    public int getMaximum() {
        return this.maximum;
    }
    
    @Override
    public Integer parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final int result = reader.readInt();
        if (result < this.minimum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, result, this.minimum);
        }
        if (result > this.maximum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, result, this.maximum);
        }
        return result;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegerArgumentType)) {
            return false;
        }
        final IntegerArgumentType that = (IntegerArgumentType)o;
        return this.maximum == that.maximum && this.minimum == that.minimum;
    }
    
    @Override
    public int hashCode() {
        return 31 * this.minimum + this.maximum;
    }
    
    @Override
    public String toString() {
        if (this.minimum == Integer.MIN_VALUE && this.maximum == Integer.MAX_VALUE) {
            return "integer()";
        }
        if (this.maximum == Integer.MAX_VALUE) {
            return "integer(" + this.minimum + ")";
        }
        return "integer(" + this.minimum + ", " + this.maximum + ")";
    }
    
    @Override
    public Collection<String> getExamples() {
        return IntegerArgumentType.EXAMPLES;
    }
    
    static {
        EXAMPLES = Arrays.asList("0", "123", "-123");
    }
}
