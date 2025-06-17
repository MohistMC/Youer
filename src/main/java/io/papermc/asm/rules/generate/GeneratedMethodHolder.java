package io.papermc.asm.rules.generate;

import io.papermc.asm.rules.RewriteRule;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

public interface GeneratedMethodHolder {

    static void loadParameters(final GeneratorAdapter adapter, final MethodTypeDesc descriptor) {
        for (int i = 0; i < descriptor.parameterCount(); i++) {
            adapter.loadArg(i);
        }
    }

    default GeneratorAdapter createAdapter(final RewriteRule.GeneratorAdapterFactory factory, final MethodCallData modified) {
        return factory.create(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC, modified.name(), modified.descriptor().descriptorString());
    }

    /**
     * Generates a method with the provided information.
     *
     * @param factory the factory for method generation
     * @param modified the method call information that replaced the matching method in bytecode
     * @param original the original method that was matched in the bytecode
     */
    void generateMethod(RewriteRule.GeneratorAdapterFactory factory, MethodCallData modified, MethodCallData original);

    interface CallData {
        int opcode();
    }

    record MethodCallData(int opcode, ClassDesc owner, String name, MethodTypeDesc descriptor, boolean isInvokeDynamic) implements CallData {

        public MethodCallData withNamePrefix(final String prefix) {
            return new MethodCallData(this.opcode(), this.owner(), prefix + this.name(), this.descriptor(), this.isInvokeDynamic());
        }
    }

    void generateConstructor(RewriteRule.GeneratorAdapterFactory factory, MethodCallData modified, ConstructorCallData original);

    record ConstructorCallData(int opcode, ClassDesc owner, MethodTypeDesc descriptor) implements CallData {
    }
}
