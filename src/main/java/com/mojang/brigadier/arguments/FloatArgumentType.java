package com.mojang.brigadier.arguments;

import java.util.Arrays;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;

public class FloatArgumentType implements ArgumentType<Float>
{
    private static final Collection<String> EXAMPLES;
    private final float minimum;
    private final float maximum;
    
    private FloatArgumentType(final float minimum, final float maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }
    
    public static FloatArgumentType floatArg() {
        return floatArg(-3.4028235E38f);
    }
    
    public static FloatArgumentType floatArg(final float min) {
        return floatArg(min, Float.MAX_VALUE);
    }
    
    public static FloatArgumentType floatArg(final float min, final float max) {
        return new FloatArgumentType(min, max);
    }
    
    public static float getFloat(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Float.class);
    }
    
    public float getMinimum() {
        return this.minimum;
    }
    
    public float getMaximum() {
        return this.maximum;
    }
    
    @Override
    public Float parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final float result = reader.readFloat();
        if (result < this.minimum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(reader, result, this.minimum);
        }
        if (result > this.maximum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(reader, result, this.maximum);
        }
        return result;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FloatArgumentType)) {
            return false;
        }
        final FloatArgumentType that = (FloatArgumentType)o;
        return this.maximum == that.maximum && this.minimum == that.minimum;
    }
    
    @Override
    public int hashCode() {
        return (int)(31.0f * this.minimum + this.maximum);
    }
    
    @Override
    public String toString() {
        if (this.minimum == -3.4028235E38f && this.maximum == Float.MAX_VALUE) {
            return "float()";
        }
        if (this.maximum == Float.MAX_VALUE) {
            return "float(" + this.minimum + ")";
        }
        return "float(" + this.minimum + ", " + this.maximum + ")";
    }
    
    @Override
    public Collection<String> getExamples() {
        return FloatArgumentType.EXAMPLES;
    }
    
    static {
        EXAMPLES = Arrays.asList("0", "1.2", ".5", "-1", "-.5", "-1234.56");
    }
}
