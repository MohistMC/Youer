package io.papermc.asm.rules.method;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import io.papermc.asm.rules.method.rewrite.ConstructorRewrite;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import io.papermc.asm.rules.method.rewrite.SimpleRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.papermc.asm.util.DescriptorUtils.toOwner;
import static io.papermc.asm.util.OpcodeUtils.isInterface;
import static io.papermc.asm.util.OpcodeUtils.isSpecial;
import static io.papermc.asm.util.OpcodeUtils.isStatic;
import static io.papermc.asm.util.OpcodeUtils.isVirtual;
import static io.papermc.asm.util.OpcodeUtils.staticOp;

public interface StaticRewrite extends MethodRewriteRule {

    String CONSTRUCTOR_METHOD_NAME = "<init>";

    ClassDesc staticRedirectOwner(final ClassProcessingContext context);

    /**
     * Transforms the intermediate descriptor to the final
     * descriptor that will be used in the rewritten bytecode.
     * <p>
     * Intermediate means that it has been modified from the
     * original accounting for the virtual/interface/static/constructor-ness
     * of the method call.
     * </p>
     *
     * @param intermediateDescriptor the intermediate descriptor
     * @return the final descriptor to be used in the rewritten bytecode
     */
    default MethodTypeDesc transformToRedirectDescriptor(final MethodTypeDesc intermediateDescriptor) {
        return intermediateDescriptor;
    }

    /**
     * Creates a rewrite for the given method call data.
     *
     * @param context the context
     * @param intermediateDescriptor the descriptor modified from the original to account for the method call type (interface, virtual, static, etc.)
     * @param originalCallData the original method call data
     * @return the rewrite
     */
    default MethodRewrite<GeneratedMethodHolder.MethodCallData> createRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final GeneratedMethodHolder.MethodCallData originalCallData) {
        return new SimpleRewrite(staticOp(originalCallData.isInvokeDynamic()), this.staticRedirectOwner(context), originalCallData.name(), this.transformToRedirectDescriptor(intermediateDescriptor), false, originalCallData.isInvokeDynamic());
    }

    /**
     * Creates a rewrite for the given constructor call data.
     *
     * @param context the context
     * @param intermediateDescriptor the descriptor modified from the original to include a return type
     * @param originalCallData the original constructor call data
     * @return the rewrite
     */
    default MethodRewrite<GeneratedMethodHolder.ConstructorCallData> createConstructorRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final GeneratedMethodHolder.ConstructorCallData originalCallData) {
        final String ownerString = toOwner(originalCallData.owner());
        final String staticMethodName = "create" + ownerString.substring(ownerString.lastIndexOf('/') + 1);
        return new ConstructorRewrite(this.staticRedirectOwner(context), toOwner(originalCallData.owner()), staticMethodName, this.transformToRedirectDescriptor(intermediateDescriptor));
    }

    @Override
    default @Nullable MethodRewrite<?> rewrite(final ClassProcessingContext context, final boolean isInvokeDynamic, final int opcode, final ClassDesc owner, final String name, final MethodTypeDesc descriptor, final boolean isInterface) {
        MethodTypeDesc modifiedDescriptor = descriptor;
        if (isVirtual(opcode, isInvokeDynamic) || isInterface(opcode, isInvokeDynamic)) { // insert owner object as first param
            modifiedDescriptor = modifiedDescriptor.insertParameterTypes(0, owner);
        } else if (isSpecial(opcode, isInvokeDynamic)) {
            if (CONSTRUCTOR_METHOD_NAME.equals(name)) {
                modifiedDescriptor = modifiedDescriptor.changeReturnType(owner);
                return this.createConstructorRewrite(context, modifiedDescriptor, new GeneratedMethodHolder.ConstructorCallData(opcode, owner, descriptor));
            } else {
                throw new UnsupportedOperationException("Unhandled static rewrite: " + opcode + " " + owner + " " + name + " " + descriptor);
            }
        } else if (!isStatic(opcode, isInvokeDynamic)) {
            throw new UnsupportedOperationException("Unhandled static rewrite: " + opcode + " " + owner + " " + name + " " + descriptor);
        }
        return this.createRewrite(context, modifiedDescriptor, new GeneratedMethodHolder.MethodCallData(opcode, owner, name, descriptor, isInvokeDynamic));
    }
}
