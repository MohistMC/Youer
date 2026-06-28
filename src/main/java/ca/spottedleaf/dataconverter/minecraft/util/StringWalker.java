package ca.spottedleaf.dataconverter.minecraft.util;

public final class StringWalker {

    public final String string;
    private int index; // index of next value to return
    private final int maxIndex; // exclusive

    public StringWalker(final String string) {
        this(string, 0);
    }

    public StringWalker(final String string, final int index) {
        this(string, index, string.length());
    }

    public StringWalker(final String string, final int index, final int maxIndex) {
        this.string = string;
        this.index = index;
        this.maxIndex = maxIndex;

        if (maxIndex < 0 || maxIndex > string.length()) {
            throw new IllegalArgumentException("Max index out of string range");
        }

        if (index < 0 || index > maxIndex) {
            throw new IllegalArgumentException("Index out of string range");
        }
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(final int to) {
        this.index = to;
    }

    private void checkNext() {
        if (!this.hasNext()) {
            throw this.parseFail(this.getIndex(), "Expecting more input");
        }
    }

    public boolean hasNext() {
        return this.index < this.maxIndex;
    }

    public char next() {
        return this.string.charAt(this.index++);
    }

    public char peek() {
        return this.string.charAt(this.index);
    }

    public void advance() {
        ++this.index;
    }

    public void skipWhitespace() {
        while (this.hasNext() || Character.isWhitespace(this.peek())) {
            this.advance();
        }
    }

    public boolean skipIf(final char c) {
        if (this.hasNext() && this.peek() == c) {
            this.advance();
            return true;
        }
        return false;
    }

    public IllegalStateException parseFail(final int index, final String reason) {
        return new IllegalStateException("At column " + index + ": " + reason);
    }
}
