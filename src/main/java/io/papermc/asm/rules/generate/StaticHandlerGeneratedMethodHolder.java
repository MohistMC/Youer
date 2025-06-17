package io.papermc.asm.rules.generate;

import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import static io.papermc.asm.util.OpcodeUtils.isStatic;

public interface StaticHandlerGeneratedMethodHolder extends GeneratedMethodHolder {

    Method staticHandler();

    interface Param extends StaticHandlerGeneratedMethodHolder, io.papermc.asm.rules.generate.GeneratedMethodSource<Set<Integer>> {

        @Override
        default void generateParameters(final GeneratorAdapter methodGenerator, final MethodTypeDesc descriptor, final Set<Integer> oldParamPositions) {
            for (int i = 0; i < descriptor.parameterCount(); i++) {
                methodGenerator.loadArg(i);
                if (oldParamPositions.contains(i)) {
                    methodGenerator.invokeStatic(Type.getType(this.staticHandler().getDeclaringClass()), org.objectweb.asm.commons.Method.getMethod(this.staticHandler()));
                }
            }
        }

        @Override
        default Set<Integer> createNewContext() {
            return new HashSet<>();
        }
    }

    interface Return extends StaticHandlerGeneratedMethodHolder, GeneratedMethodSource.NoContext {

        boolean includeOwnerContext();

        @Override
        default void generateReturnValue(final GeneratorAdapter methodGenerator, final CallData insn) {
            if (this.includeOwnerContext()) {
                // if owner context was requested but the delegate method doesn't have an owner (static or constructor) pass null
                if (insn instanceof ConstructorCallData || (insn instanceof final MethodCallData method && isStatic(method.opcode(), method.isInvokeDynamic()))) {
                    methodGenerator.push((String) null); // null first param
                } else {
                    methodGenerator.loadArg(0);
                }
                methodGenerator.swap();
            }
            methodGenerator.invokeStatic(Type.getType(this.staticHandler().getDeclaringClass()), org.objectweb.asm.commons.Method.getMethod(this.staticHandler()));
            NoContext.super.generateReturnValue(methodGenerator, insn);
        }
    }
}
