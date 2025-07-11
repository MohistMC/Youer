package io.papermc.asm.rules.builder.matcher.method;

public interface MethodTypeMatcherBuilder<B> {

    default B virtual() {
        return this.type(io.papermc.asm.rules.builder.matcher.method.MethodType.VIRTUAL);
    }

    default B statik() {
        return this.type(io.papermc.asm.rules.builder.matcher.method.MethodType.STATIC);
    }

    default B itf() {
        return this.type(io.papermc.asm.rules.builder.matcher.method.MethodType.INTERFACE);
    }

    B type(final MethodType... types);
}
