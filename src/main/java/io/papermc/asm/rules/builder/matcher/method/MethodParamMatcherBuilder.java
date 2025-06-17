package io.papermc.asm.rules.builder.matcher.method;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.function.Predicate;

public interface MethodParamMatcherBuilder<B> {

    default B hasParam(final ClassDesc paramClassDesc) {
        return this.desc(d -> d.parameterList().contains(paramClassDesc));
    }

    default B hasParam(final ClassDesc paramClassDesc, final int paramIdx) {
        return this.desc(d -> d.parameterType(paramIdx).equals(paramClassDesc));
    }

    default B hasReturn(final ClassDesc returnClassDesc) {
        return this.desc(d -> d.returnType().equals(returnClassDesc));
    }

    default B desc(final String... descriptors) {
        return this.desc(desc -> Arrays.stream(descriptors).anyMatch(d -> desc.descriptorString().equals(d)));
    }

    default B desc(final MethodTypeDesc... descriptors) {
        return this.desc(desc -> Arrays.asList(descriptors).contains(desc));
    }

    B desc(final Predicate<? super MethodTypeDesc> descPredicate);
}
