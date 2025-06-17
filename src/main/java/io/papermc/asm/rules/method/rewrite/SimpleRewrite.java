package io.papermc.asm.rules.method.rewrite;

import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import static io.papermc.asm.util.DescriptorUtils.toOwner;

/**
 * Holds the structure of the rewritten method that replaces a matching method found in the bytecode. This
 * is for non-constructors. For constructors, use {@link ConstructorRewrite}.
 *
 * @param opcode          the replaced opcode
 * @param owner           the replaced owner
 * @param name            the replaced name
 * @param descriptor      the replaced descriptor
 * @param isInterface     if the replaced method is an interface method
 * @param isInvokeDynamic if the replaced method is an invokedynamic
 * @param generatorInfo   info for generating the method (optional)
 */
public record SimpleRewrite(
    int opcode,
    ClassDesc owner,
    String name,
    MethodTypeDesc descriptor,
    boolean isInterface,
    boolean isInvokeDynamic,
    io.papermc.asm.rules.method.rewrite.MethodRewrite.GeneratorInfo<GeneratedMethodHolder.MethodCallData> generatorInfo,
    @Nullable Consumer<Object[]> handleExtras
) implements io.papermc.asm.rules.method.rewrite.MethodRewrite<GeneratedMethodHolder.MethodCallData> {

    public SimpleRewrite(final int opcode, final ClassDesc owner, final String name, final MethodTypeDesc descriptor, final boolean isInterface, final boolean isInvokeDynamic) {
        this(opcode, owner, name, descriptor, isInterface, isInvokeDynamic, null, null);
    }

    @Override
    public void apply(final MethodVisitor delegate, final MethodNode context) {
        delegate.visitMethodInsn(this.opcode(), toOwner(this.owner()), this.name(), this.descriptor().descriptorString(), this.isInterface());
    }

    @Override
    public void applyToBootstrapArguments(final Object[] arguments) {
        arguments[BOOTSTRAP_HANDLE_IDX] = new Handle(this.opcode(), toOwner(this.owner()), this.name(), this.descriptor().descriptorString(), this.isInterface());
        if (this.handleExtras != null) {
            this.handleExtras.accept(arguments);
        }
    }

    @Override
    public io.papermc.asm.rules.method.rewrite.MethodRewrite<GeneratedMethodHolder.MethodCallData> withGeneratorInfo(final GeneratedMethodHolder holder, final GeneratedMethodHolder.MethodCallData original) {
        if (this.generatorInfo != null) {
            throw new IllegalStateException("Generator info already set");
        }
        final String methodName = GENERATED_PREFIX + toOwner(original.owner()).replace('/', '_') + "$" + this.name();
        return new SimpleRewrite(this.opcode(), this.owner(), methodName, this.descriptor(), this.isInterface(), this.isInvokeDynamic(), new GeneratorInfo<>(holder, original), this.handleExtras());
    }

    @Override
    public MethodRewrite<GeneratedMethodHolder.MethodCallData> withHandleExtras(final Consumer<Object[]> extras) {
        return new SimpleRewrite(this.opcode(), this.owner(), this.name(), this.descriptor(), this.isInterface(), this.isInvokeDynamic(), this.generatorInfo(), extras);
    }

    @Override
    public @Nullable MethodGenerator createMethodGenerator() {
        if (this.generatorInfo == null) {
            return null;
        }
        final GeneratedMethodHolder.MethodCallData original = this.generatorInfo.original();
        return factory -> {
            this.generatorInfo.holder().generateMethod(
                factory,
                new GeneratedMethodHolder.MethodCallData(Opcodes.INVOKESTATIC, this.owner(), this.name(), this.descriptor(), this.isInvokeDynamic()),
                original
            );
        };
    }
}
