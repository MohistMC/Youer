package com.mojang.brigadier.arguments;

import java.util.Arrays;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;

public class DoubleArgumentType implements ArgumentType<Double>
{
    private static final Collection<String> EXAMPLES;
    private final double minimum;
    private final double maximum;
    
    private DoubleArgumentType(final double minimum, final double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }
    
    public static DoubleArgumentType doubleArg() {
        return doubleArg(-1.7976931348623157E308);
    }
    
    public static DoubleArgumentType doubleArg(final double min) {
        return doubleArg(min, Double.MAX_VALUE);
    }
    
    public static DoubleArgumentType doubleArg(final double min, final double max) {
        return new DoubleArgumentType(min, max);
    }
    
    public static double getDouble(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Double.class);
    }
    
    public double getMinimum() {
        return this.minimum;
    }
    
    public double getMaximum() {
        return this.maximum;
    }
    
    @Override
    public Double parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final double result = reader.readDouble();
        if (result < this.minimum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, result, this.minimum);
        }
        if (result > this.maximum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooHigh().createWithContext(reader, result, this.maximum);
        }
        return result;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoubleArgumentType)) {
            return false;
        }
        final DoubleArgumentType that = (DoubleArgumentType)o;
        return this.maximum == that.maximum && this.minimum == that.minimum;
    }
    
    @Override
    public int hashCode() {
        return (int)(31.0 * this.minimum + this.maximum);
    }
    
    @Override
    public String toString() {
        if (this.minimum == -1.7976931348623157E308 && this.maximum == Double.MAX_VALUE) {
            return "double()";
        }
        if (this.maximum == Double.MAX_VALUE) {
            return "double(" + this.minimum + ")";
        }
        return "double(" + this.minimum + ", " + this.maximum + ")";
    }
    
    @Override
    public Collection<String> getExamples() {
        return DoubleArgumentType.EXAMPLES;
    }
    
    static {
        EXAMPLES = Arrays.asList("0", "1.2", ".5", "-1", "-.5", "-1234.56");
    }
}
