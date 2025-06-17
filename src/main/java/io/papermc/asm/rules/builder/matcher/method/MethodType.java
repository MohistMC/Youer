package io.papermc.asm.rules.builder.matcher.method;

import io.papermc.asm.util.OpcodeUtils;
import java.util.function.BiPredicate;

public enum MethodType {
    VIRTUAL(OpcodeUtils::isVirtual),
    INTERFACE(OpcodeUtils::isInterface),
    STATIC(OpcodeUtils::isStatic),
    SPECIAL(OpcodeUtils::isSpecial),
    DYNAMIC(OpcodeUtils::isDynamic);

    private final BiPredicate<Integer, Boolean> matcher;

    MethodType(final BiPredicate<Integer, Boolean> matcher) {
        this.matcher = matcher;
    }

    public boolean matches(final int opcode, final boolean isInvokeDynamic) {
        return this.matcher.test(opcode, isInvokeDynamic);
    }
}
