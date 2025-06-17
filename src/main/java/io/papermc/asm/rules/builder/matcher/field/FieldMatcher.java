package io.papermc.asm.rules.builder.matcher.field;

@FunctionalInterface
public interface FieldMatcher {

    static FieldMatcherBuilderImpl builder() {
        return new FieldMatcherBuilderImpl();
    }

    boolean matches(String name, String descriptor);

    default FieldMatcher or(final FieldMatcher other) {
        return (name, descriptor) -> this.matches(name, descriptor) || other.matches(name, descriptor);
    }

}
