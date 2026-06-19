package com.mojang.math;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

public class Divisor implements IntIterator {
    private final int denominator;
    private final int quotient;
    private final int mod;
    private int returnedParts;
    private int remainder;

    public Divisor(final int numerator, final int denominator) {
        this.denominator = denominator;
        if (denominator > 0) {
            this.quotient = numerator / denominator;
            this.mod = numerator % denominator;
        } else {
            this.quotient = 0;
            this.mod = 0;
        }
    }

    @Override
    public boolean hasNext() {
        return this.returnedParts < this.denominator;
    }

    @Override
    public int nextInt() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        int next = this.quotient;
        this.remainder = this.remainder + this.mod;
        if (this.remainder >= this.denominator) {
            this.remainder = this.remainder - this.denominator;
            next++;
        }

        this.returnedParts++;
        return next;
    }

    @VisibleForTesting
    public static Iterable<Integer> asIterable(final int numerator, final int denominator) {
        return () -> new Divisor(numerator, denominator);
    }
}
