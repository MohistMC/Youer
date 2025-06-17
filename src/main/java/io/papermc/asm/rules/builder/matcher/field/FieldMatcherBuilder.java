package io.papermc.asm.rules.builder.matcher.field;

import io.papermc.asm.util.Builder;
import java.lang.constant.ClassDesc;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static java.util.function.Predicate.isEqual;

public interface FieldMatcherBuilder extends Builder<FieldMatcher> {

    default FieldMatcherBuilder match(final String fieldName) {
        return this.match(fieldName, $ -> true);
    }

    default FieldMatcherBuilder match(final String fieldName, final ClassDesc fieldDesc) {
        return this.match(fieldName, isEqual(fieldDesc));
    }

    default FieldMatcherBuilder match(final String fieldName, final Predicate<ClassDesc> fieldDescPredicate) {
        return this.match(isEqual(fieldName), fieldDescPredicate);
    }

    default FieldMatcherBuilder match(final Collection<String> fieldNames) {
        return this.match(fieldNames, $ -> true);
    }

    default FieldMatcherBuilder match(final Collection<String> fieldNames, final ClassDesc fieldDesc) {
        return this.match(fieldNames, isEqual(fieldDesc));
    }

    default FieldMatcherBuilder match(final Collection<String> fieldNames, final Predicate<ClassDesc> fieldDescPredicate) {
        final List<String> copy = List.copyOf(fieldNames);
        return this.match(copy::contains, fieldDescPredicate);
    }

    default FieldMatcherBuilder match(final Predicate<String> fieldNamePredicate) {
        return this.match(fieldNamePredicate, $ -> true);
    }

    default FieldMatcherBuilder match(final Predicate<String> fieldNamePredicate, final ClassDesc fieldDesc) {
        return this.match(fieldNamePredicate, isEqual(fieldDesc));
    }

    FieldMatcherBuilder match(Predicate<String> fieldNamePredicate, Predicate<ClassDesc> fieldDescPredicate);
}
