package io.papermc.asm.rules.method.rewrite;

import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import io.papermc.asm.rules.method.StaticRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayDeque;
import java.util.Deque;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static io.papermc.asm.util.DescriptorUtils.toOwner;

public record ConstructorRewrite(
    ClassDesc staticRedirectOwner,
    String constructorOwner,
    String methodName,
    MethodTypeDesc descriptor,
    io.papermc.asm.rules.method.rewrite.MethodRewrite.GeneratorInfo<GeneratedMethodHolder.ConstructorCallData> generatorInfo
) implements io.papermc.asm.rules.method.rewrite.MethodRewrite<GeneratedMethodHolder.ConstructorCallData> {

    public ConstructorRewrite(final ClassDesc staticRedirectOwner, final String constructorOwner, final String methodName, final MethodTypeDesc descriptor) {
        this(staticRedirectOwner, constructorOwner, methodName, descriptor, null);
    }

    @Override
    public void apply(final MethodVisitor delegate, final MethodNode context) {
        // the bytecode instruction sequence for constructors is:
        // NEW <owner>
        // DUP
        // <other instructions that might set up parameters>
        // INVOKESPECIAL <owner>/<init>
        // This detects that pattern and correctly removes instructions that
        // are no longer needed after the static redirect
        AbstractInsnNode insn = context.instructions.getLast();
        boolean lastInsnWasDup = false;
        boolean handled = false;
        final Deque<String> typeStack = new ArrayDeque<>();
        while (insn != null) {
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL && StaticRewrite.CONSTRUCTOR_METHOD_NAME.equals(((MethodInsnNode) insn).name)) {
                typeStack.push(((MethodInsnNode) insn).owner);
            }
            if (lastInsnWasDup && insn.getOpcode() == Opcodes.NEW) {
                final TypeInsnNode newNode = (TypeInsnNode) insn;
                if (typeStack.isEmpty()) {
                    if (!newNode.desc.equals(this.constructorOwner())) {
                        throw new IllegalStateException("typeStack was empty and the 'new' type didn't match the ctor type");
                    }
                    final AbstractInsnNode dup = insn.getNext();
                    context.instructions.remove(insn); // remove NEW
                    context.instructions.remove(dup); // remove DUP
                    handled = true;
                    break;
                } else {
                    final String top = typeStack.pop();
                    if (!newNode.desc.equals(top)) {
                        throw new IllegalStateException("typeStack top " + top + " didn't match expected " + newNode.desc + " from 'new' node");
                    }
                }
            }
            lastInsnWasDup = insn.getOpcode() == Opcodes.DUP;
            insn = insn.getPrevious();
        }
        if (!handled) {
            throw new IllegalStateException("Didn't find new/dup before invokespecial for ctor");
        }
        delegate.visitMethodInsn(Opcodes.INVOKESTATIC, toOwner(this.staticRedirectOwner()), this.methodName(), this.descriptor().descriptorString(), false);
    }

    @Override
    public void applyToBootstrapArguments(final Object[] arguments) {
        arguments[BOOTSTRAP_HANDLE_IDX] = new Handle(Opcodes.H_INVOKESTATIC, toOwner(this.staticRedirectOwner()), this.methodName(), this.descriptor().descriptorString(), false);
        // TODO not really needed on **every** rewrite, just the fuzzy param ones, but it doesn't seem to break anything since it will always be the same
        arguments[DYNAMIC_TYPE_IDX] = Type.getMethodType(this.descriptor().descriptorString());
    }

    @Override
    public MethodRewrite<GeneratedMethodHolder.ConstructorCallData> withGeneratorInfo(final GeneratedMethodHolder holder, final GeneratedMethodHolder.ConstructorCallData original) {
        final String methodName = GENERATED_PREFIX + toOwner(original.owner()).replace('/', '_') + "$" + this.methodName();
        return new ConstructorRewrite(this.staticRedirectOwner(), this.constructorOwner(), methodName, this.descriptor(), new GeneratorInfo<>(holder, original));
    }

    @Override
    public @Nullable MethodGenerator createMethodGenerator() {
        if (this.generatorInfo == null) {
            return null;
        }
        final GeneratedMethodHolder.ConstructorCallData original = this.generatorInfo.original();
        return factory -> {
            this.generatorInfo.holder().generateConstructor(
                factory,
                new GeneratedMethodHolder.MethodCallData(Opcodes.INVOKESTATIC, this.staticRedirectOwner(), this.methodName(), this.descriptor(), false),
                original
            );
        };
    }
}
