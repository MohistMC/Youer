package io.papermc.asm.rules.method.generated;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.generate.GeneratedMethodHolder;
import io.papermc.asm.rules.method.StaticRewrite;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static io.papermc.asm.util.DescriptorUtils.fromOwner;

/**
 * A static rewrite which generates the method that the bytecode will be redirected to.
 */
public interface GeneratedStaticRewrite extends StaticRewrite, GeneratedMethodHolder {

    @Override
    default ClassDesc staticRedirectOwner(final ClassProcessingContext context) {
        return fromOwner(context.processingClassName()); // create generated method in current class
    }

    @Override
    default MethodRewrite<MethodCallData> createRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final MethodCallData originalCallData) {
        return StaticRewrite.super.createRewrite(context, intermediateDescriptor, originalCallData)
            .withGeneratorInfo(this, originalCallData);
    }

    @Override
    default MethodRewrite<ConstructorCallData> createConstructorRewrite(final ClassProcessingContext context, final MethodTypeDesc intermediateDescriptor, final ConstructorCallData originalCallData) {
        return StaticRewrite.super.createConstructorRewrite(context, intermediateDescriptor, originalCallData)
            .withGeneratorInfo(this, originalCallData);
    }
}
