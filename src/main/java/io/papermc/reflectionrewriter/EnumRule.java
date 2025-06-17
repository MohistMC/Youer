package io.papermc.reflectionrewriter;

import io.papermc.asm.ClassInfo;
import io.papermc.asm.ClassInfoProvider;
import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.method.MethodMatcher;
import java.lang.constant.ClassDesc;
import java.lang.invoke.ConstantBootstraps;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Rule for rewriting enum valueOf calls. Not normally needed
 * as all common remapping software only remaps enum field names,
 * not the ldc insn for the name of the enum constant.
 *
 * <p>This rule expects the following methods to be present
 * on your reflection proxy class.</p>
 *
 * <pre>
 * public static &#60;E extends Enum&#60;E>> E enumConstant(
 *     final MethodHandles.Lookup lookup,
 *     final String name,
 *     final Class&#60;E> type
 * ) {
 *     // Implementation ...
 * }
 *
 * public static &#60;E extends Enum&#60;E>> E valueOf(
 *     final Class&#60;E> enumClass,
 *     final String name
 * ) {
 *     // Implementation...
 * }
 *
 * public static &#60;E extends Enum&#60;E>> E valueOf(
 *     final String name,
 *     final Class&#60;E> enumClass
 * ) {
 *     return valueOf(enumClass, name);
 * }
 * </pre>
 */
public final class EnumRule {
    private EnumRule() {
    }

    public static RewriteRule create(
        final String proxyClassName,
        final Predicate<String> ownerPredicate
    ) {
        final RewriteRule rewrite = RewriteRule.forOwnerClass(ConstantBootstraps.class, rf -> {
            rf.plainStaticRewrite(ClassDesc.of(proxyClassName), MethodMatcher.builder()
                .match("enumConstant", b -> b.desc("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;"))
                .build()
            );
        });
        final RewriteRule enumRule = new RewriteRule() {
            @Override
            public ClassVisitor createVisitor(final int api, final ClassVisitor parent, final ClassProcessingContext context) {
                return new ClassVisitor(api, parent) {
                    @Override
                    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                        return new EnumMethodVisitor(this.api, super.visitMethod(access, name, descriptor, signature, exceptions), proxyClassName, context.classInfoProvider(), ownerPredicate);
                    }
                };
            }
        };
        return RewriteRule.chain(rewrite, enumRule);
    }

    public static RewriteRule minecraft(final String proxyClassName) {
        return create(proxyClassName, owner -> owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/"));
    }

    // todo needs to handle invokedynamic?
    private static final class EnumMethodVisitor extends MethodVisitor {
        private final String proxy;
        private final ClassInfoProvider classInfoProvider;
        private final Predicate<String> ownerPredicate;
        private int increaseMaxStack;

        EnumMethodVisitor(final int api, final MethodVisitor parent, final String proxyClassName, final ClassInfoProvider classInfoProvider, final Predicate<String> ownerPredicate) {
            super(api, parent);
            this.proxy = proxyClassName;
            this.classInfoProvider = classInfoProvider;
            this.ownerPredicate = ownerPredicate;
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
            super.visitMaxs(maxStack + this.increaseMaxStack, maxLocals);
            this.increaseMaxStack = 0;
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            if (this.ownerPredicate.test(owner) && name.equals("valueOf") && descriptor.equals("(Ljava/lang/String;)L" + owner + ";")) {
                // Rewrite SomeEnum.valueOf(String)
                final @Nullable ClassInfo info = this.classInfoProvider.info(owner);
                if (info != null && info.isEnum()) {
                    // Increase max stack size for this method by one for the class parameter
                    // Note that in some cases this does not actually have to be increased (which we cannot track here),
                    // but a larger max stack size than needed should not create any problems
                    this.increaseMaxStack++;

                    super.visitLdcInsn(Type.getType("L" + owner + ";")); // Add the class as a parameter
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, this.proxy, name, "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;", false);
                    super.visitTypeInsn(Opcodes.CHECKCAST, owner); // Make sure we have the right type
                    return;
                }
            } else if (name.equals("valueOf") && descriptor.equals("(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;")) {
                // Rewrite AnyEnum.valueOf(Class, String)
                if (this.isEnum(owner)) {
                    super.visitMethodInsn(opcode, this.proxy, name, descriptor, false);
                    return;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        private boolean isEnum(final String owner) {
            if (owner.equals("java/lang/Enum")) {
                return true;
            }
            final @Nullable ClassInfo info = this.classInfoProvider.info(owner);
            return info != null && info.isEnum();
        }
    }
}
