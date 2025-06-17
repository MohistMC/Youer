package io.papermc.asm.rules.builder.matcher.method;

@FunctionalInterface
public interface OpcodePredicate {

    boolean matches(int opcode, boolean isInvokeDynamic);
}
