package io.papermc.asm.rules.method.rewrite;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * Represents a change/rewrite to bytecode.
 *
 * @param <D> the type of call data
 */
public interface MethodRewrite<D extends GeneratedMethodHolder.CallData> {

    int BOOTSTRAP_HANDLE_IDX = 1;
    int DYNAMIC_TYPE_IDX = 2;
    String GENERATED_PREFIX = "paperAsmGenerated$";

    /**
     * Called to apply the rewrite when a matching invoke non-dynamic instruction is found.
     *
     * @param delegate the visitor to apply the change to
     * @param context the surrounding context of the method's data including instructions
     */
    void apply(MethodVisitor delegate, MethodNode context);

    /**
     * Called to apply the rewrite to a matching invokedynamic instruction is found.
     *
     * @param arguments the bootstrap arguments {@link MethodVisitor#visitInvokeDynamicInsn(String, String, Handle, Object...)}
     */
    void applyToBootstrapArguments(Object[] arguments);

    /**
     * Creates a new {@link MethodRewrite} with the given generator info.
     *
     * @param holder the generator holder
     * @param original the original call data
     * @return the new rewrite
     */
    MethodRewrite<D> withGeneratorInfo(GeneratedMethodHolder holder, D original);

    /**
     * Creates a new {@link MethodRewrite} with the given "extras" handler. This
     * is when extra changes are required for invokedynamic instructions.
     *
     * @param extras the extras handler
     * @return the new rewrite (or the same if not supported on the rewrite type)
     */
    default MethodRewrite<D> withHandleExtras(final Consumer<Object[]> extras) {
        return this;
    }

    /**
     * Creates a {@link MethodGenerator} for this rewrite.
     *
     * @return a method generator, or null if this rewrite doesn't have one
     * @see #withGeneratorInfo(GeneratedMethodHolder, GeneratedMethodHolder.CallData)
     */
    @Nullable MethodGenerator createMethodGenerator();

    @FunctionalInterface
    interface MethodGenerator {

        void generate(RewriteRule.GeneratorAdapterFactory factory);
    }

    record GeneratorInfo<D extends GeneratedMethodHolder.CallData>(GeneratedMethodHolder holder, D original) {
    }
}
