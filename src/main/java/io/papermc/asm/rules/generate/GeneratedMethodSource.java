package io.papermc.asm.rules.generate;

import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.method.StaticRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static io.papermc.asm.util.OpcodeUtils.isInterface;
import static io.papermc.asm.util.OpcodeUtils.isStatic;
import static io.papermc.asm.util.OpcodeUtils.isVirtual;

public interface GeneratedMethodSource<C> extends io.papermc.asm.rules.generate.GeneratedMethodHolder {

    /**
     * Transforms the descriptor of the generated method to the descriptor
     * of the method that will be called within the generated method. This
     * should be the method that actually exists in source.
     *
     * @param original the method descriptor of the generated method that the bytecode was redirected to
     * @param context context for the generator
     * @return the descriptor of the method that will be called inside the generated method
     */
    MethodTypeDesc transformInvokedDescriptor(final MethodTypeDesc original, final C context);

    default void generateParameters(final GeneratorAdapter methodGenerator, final MethodTypeDesc descriptor, final C context) {
        GeneratedMethodHolder.loadParameters(methodGenerator, descriptor);
    }

    @Override
    default void generateConstructor(final RewriteRule.GeneratorAdapterFactory factory, final MethodCallData modified, final ConstructorCallData original) {
        final ClassDesc methodOwner = original.owner();
        final C context = this.createNewContext();
        final GeneratorAdapter methodGenerator = this.createAdapter(factory, modified);
        final MethodTypeDesc transformedInvokedDescriptor = this.transformInvokedDescriptor(modified.descriptor(), context);
        final Type type = Type.getType(methodOwner.descriptorString());
        methodGenerator.newInstance(type);
        methodGenerator.dup();
        this.generateParameters(methodGenerator, modified.descriptor(), context);
        // change return type to VOID because we are calling a constructor
        methodGenerator.invokeConstructor(type, new Method(StaticRewrite.CONSTRUCTOR_METHOD_NAME, transformedInvokedDescriptor.changeReturnType(ConstantDescs.CD_void).descriptorString()));
        this.generateReturnValue(methodGenerator, original);
        methodGenerator.endMethod();
    }

    @Override
    default void generateMethod(final RewriteRule.GeneratorAdapterFactory factory, final MethodCallData modified, final MethodCallData original) {
        final ClassDesc methodOwner = original.owner();
        final C context = this.createNewContext();
        final GeneratorAdapter methodGenerator = this.createAdapter(factory, modified);
        MethodTypeDesc transformedInvokedDescriptor = this.transformInvokedDescriptor(modified.descriptor(), context);
        final boolean isInterfaceCall = isInterface(original.opcode(), original.isInvokeDynamic());
        if (isVirtual(original.opcode(), original.isInvokeDynamic()) || isInterfaceCall) {
            transformedInvokedDescriptor = transformedInvokedDescriptor.dropParameterTypes(0, 1); // remove inserted param if virtual or interface
        }
        this.generateParameters(methodGenerator, modified.descriptor(), context);
        final Method method = new Method(original.name(), transformedInvokedDescriptor.descriptorString());
        final Type originalOwner = Type.getType(methodOwner.descriptorString());
        final boolean isStaticCall = isStatic(original.opcode(), original.isInvokeDynamic());
        if (isInterfaceCall) {
            methodGenerator.invokeInterface(originalOwner, method);
        } else if (!isStaticCall) {
            methodGenerator.invokeVirtual(originalOwner, method);
        } else {
            methodGenerator.invokeStatic(originalOwner, method);
        }
        this.generateReturnValue(methodGenerator, original);
        methodGenerator.endMethod();
    }

    // @MustBeInvokedByOverriders
    default void generateReturnValue(final GeneratorAdapter methodGenerator, final CallData insn) {
        methodGenerator.returnValue();
    }

    C createNewContext();

    interface NoContext extends GeneratedMethodSource<Void> {

        @Override
        default Void createNewContext() {
            return null;
        }
    }
}
