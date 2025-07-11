package io.papermc.asm.rules.builder.matcher.field;

import java.lang.constant.ClassDesc;
import java.util.function.Predicate;

public final class FieldMatcherBuilderImpl implements io.papermc.asm.rules.builder.matcher.field.FieldMatcherBuilder {

    private io.papermc.asm.rules.builder.matcher.field.FieldMatcher matcher = (name, descriptor) -> false;

    FieldMatcherBuilderImpl() {
    }

    @Override
    public FieldMatcherBuilder match(final Predicate<String> fieldNamePredicate, final Predicate<ClassDesc> fieldDescPredicate) {
        this.matcher = this.matcher.or((name, descriptor) -> fieldNamePredicate.test(name) && fieldDescPredicate.test(ClassDesc.ofDescriptor(descriptor)));
        return this;
    }

    @Override
    public FieldMatcher build() {
        return this.matcher;
    }
}
