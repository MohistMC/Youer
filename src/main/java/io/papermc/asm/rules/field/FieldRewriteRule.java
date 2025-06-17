package io.papermc.asm.rules.field;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import java.lang.constant.ClassDesc;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static io.papermc.asm.util.DescriptorUtils.classDesc;

public interface FieldRewriteRule extends RewriteRule {

    default boolean shouldProcess(final ClassProcessingContext context, final int opcode, final String owner, final String name, final String descriptor) {
        return true;
    }

    @Override
    default ClassVisitor createVisitor(final int api, final ClassVisitor parent, final ClassProcessingContext context) {
        return new ClassVisitor(api, parent) {
            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                return new MethodVisitor(this.api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
                        if (FieldRewriteRule.this.shouldProcess(context, opcode, owner, name, descriptor)) {
                            final @Nullable Rewrite rewrite = FieldRewriteRule.this.rewrite(context, opcode, owner, name, classDesc(descriptor));
                            if (rewrite != null) {
                                rewrite.apply(this.getDelegate());
                                return;
                            }
                        }
                        super.visitFieldInsn(opcode, owner, name, descriptor);
                    }
                };
            }
        };
    }

    @Nullable Rewrite rewrite(ClassProcessingContext context, int opcode, String owner, String name, ClassDesc fieldTypeDesc);

    interface Rewrite {
        void apply(MethodVisitor delegate);
    }

    record SimpleRewrite(int opcode, String owner, String name, ClassDesc fieldTypeDesc) implements Rewrite {

        @Override
        public void apply(final MethodVisitor delegate) {
            delegate.visitFieldInsn(this.opcode, this.owner, this.name, this.fieldTypeDesc.descriptorString());
        }
    }
}
