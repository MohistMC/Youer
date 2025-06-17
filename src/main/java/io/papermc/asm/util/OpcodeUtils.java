package io.papermc.asm.util;

import org.objectweb.asm.Opcodes;

public final class OpcodeUtils {

    public static boolean isVirtual(final int opcode, final boolean invokeDynamic) {
        return opcode == (invokeDynamic ? Opcodes.H_INVOKEVIRTUAL : Opcodes.INVOKEVIRTUAL);
    }

    public static boolean isStatic(final int opcode, final boolean invokeDynamic) {
        return opcode == (invokeDynamic ? Opcodes.H_INVOKESTATIC : Opcodes.INVOKESTATIC);
    }

    public static boolean isSpecial(final int opcode, final boolean invokeDynamic) {
        if (invokeDynamic) {
            return opcode == Opcodes.H_INVOKESPECIAL || opcode == Opcodes.H_NEWINVOKESPECIAL;
        }
        return opcode == Opcodes.INVOKESPECIAL;
    }

    public static boolean isInterface(final int opcode, final boolean invokeDynamic) {
        return opcode == (invokeDynamic ? Opcodes.H_INVOKEINTERFACE : Opcodes.INVOKEINTERFACE);
    }

    public static boolean isDynamic(final int opcode, final boolean invokeDynamic) {
        return !invokeDynamic && opcode == Opcodes.INVOKEDYNAMIC;
    }

    public static int virtualOp(final boolean invokeDynamic) {
        return invokeDynamic ? Opcodes.H_INVOKEVIRTUAL : Opcodes.INVOKEVIRTUAL;
    }

    public static int staticOp(final boolean invokeDynamic) {
        return invokeDynamic ? Opcodes.H_INVOKESTATIC : Opcodes.INVOKESTATIC;
    }

    public static int interfaceOp(final boolean invokeDynamic) {
        return invokeDynamic ? Opcodes.H_INVOKEINTERFACE : Opcodes.INVOKEINTERFACE;
    }

    private OpcodeUtils() {
    }
}
