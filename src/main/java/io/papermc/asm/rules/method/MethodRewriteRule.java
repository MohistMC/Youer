package io.papermc.asm.rules.method;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.MethodNode;

import static io.papermc.asm.util.DescriptorUtils.fromOwner;
import static io.papermc.asm.util.DescriptorUtils.methodDesc;

public interface MethodRewriteRule extends RewriteRule {

    String LAMBDA_METAFACTORY_OWNER = "java/lang/invoke/LambdaMetafactory";

    /**
     * Checks if this rule should even attempt to create a {@link MethodRewrite} for the current context.
     * Returning true here does <b>not</b> mean a {@link MethodRewrite} will be created, but it's an early exit
     * for rules that know they won't be able to rewrite the method.
     *
     * @param context the current context
     * @param opcode the method opcode
     * @param owner the method owner (with slashes)
     * @param name the method name
     * @param descriptor the method descriptor
     * @param isInterface if the owning class is an interface
     * @param isInvokeDynamic if the method call is from an invokedynamic instruction
     * @return true to continue processing the instruction
     */
    boolean shouldProcess(final ClassProcessingContext context, final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface, final boolean isInvokeDynamic);

    /**
     * Creates a {@link MethodRewrite} which will be applied to the processed bytecode.
     *
     * @param context the current context
     * @param isInvokeDynamic if this is in an invokedynamic instruction
     * @param opcode the opcode of the invoke instruction or the invokedynamic handle tag
     * @param owner the owner of the invoke method or the owner of the invokedynamic handle
     * @param name the name of the invoke method or the name of the invokedynamic handle
     * @param descriptor the descriptor of the invoke method or the descriptor of the invokedynamic handle
     * @param isInterface if the method is an interface method
     * @return the rewrite or null if no rewrite should be applied
     */
    @Nullable
    MethodRewrite<?> rewrite(ClassProcessingContext context, boolean isInvokeDynamic, int opcode, ClassDesc owner, String name, MethodTypeDesc descriptor, boolean isInterface);

    @Override
    default ClassVisitor createVisitor(final int api, final ClassVisitor parent, final ClassProcessingContext context) {
        record MethodKey(String owner, String name, MethodTypeDesc descriptor) implements Comparable<MethodKey> {
            private static final Comparator<MethodKey> COMPARATOR = Comparator.comparing(MethodKey::owner)
                .thenComparing(MethodKey::name)
                .thenComparing(key -> key.descriptor().descriptorString());

            @Override
            public int compareTo(final MethodKey o) {
                return COMPARATOR.compare(this, o);
            }
        }
        final Map<MethodKey, MethodRewrite.MethodGenerator> methodsToGenerate = new TreeMap<>();
        return new ClassVisitor(api, parent) {

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                final MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                final MethodNode mn = new MethodNode(this.api, access, name, descriptor, signature, exceptions);
                return new MethodVisitor(this.api, mn) {
                    @Override
                    public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
                        if (MethodRewriteRule.this.shouldProcess(context, opcode, owner, name, descriptor, isInterface, false)) {
                            final ClassDesc methodOwner = fromOwner(owner);
                            final MethodTypeDesc methodDesc = methodDesc(descriptor);
                            final @Nullable MethodRewrite<?> rewrite = MethodRewriteRule.this.rewrite(context, false, opcode, methodOwner, name, methodDesc, isInterface);
                            if (rewrite != null) {
                                rewrite.apply(this.getDelegate(), mn);
                                final MethodRewrite.@Nullable MethodGenerator willGenerate = rewrite.createMethodGenerator();
                                if (willGenerate != null) {
                                    methodsToGenerate.put(new MethodKey(owner, name, methodDesc), willGenerate);
                                }
                                return;
                            }
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
                        if (LAMBDA_METAFACTORY_OWNER.equals(bootstrapMethodHandle.getOwner()) && bootstrapMethodArguments.length > 1 && bootstrapMethodArguments[1] instanceof final Handle handle) {
                            if (MethodRewriteRule.this.shouldProcess(context, handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface(), true)) {
                                final ClassDesc handleOwner = fromOwner(handle.getOwner());
                                final MethodTypeDesc handleDesc = methodDesc(handle.getDesc());
                                final @Nullable MethodRewrite<?> rewrite = MethodRewriteRule.this.rewrite(context, true, handle.getTag(), handleOwner, handle.getName(), handleDesc, handle.isInterface());
                                if (rewrite != null) {
                                    rewrite.applyToBootstrapArguments(bootstrapMethodArguments);
                                    final MethodRewrite.@Nullable MethodGenerator willGenerate = rewrite.createMethodGenerator();
                                    if (willGenerate != null) {
                                        methodsToGenerate.put(new MethodKey(handle.getOwner(), handle.getName(), handleDesc), willGenerate);
                                    }
                                }
                            }
                        }
                        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                    }

                    @Override
                    public void visitEnd() {
                        mn.accept(methodVisitor); // write possibly modified MethodNode
                        super.visitEnd();
                    }
                };
            }

            @Override
            public void visitEnd() {
                final GeneratorAdapterFactory factory = (access, name, descriptor) -> {
                    final MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, null, null);
                    return new GeneratorAdapter(methodVisitor, access, name, descriptor);
                };
                for (final MethodRewrite.MethodGenerator consumer : methodsToGenerate.values()) {
                    consumer.generate(factory);
                }
                super.visitEnd();
            }
        };
    }
}
