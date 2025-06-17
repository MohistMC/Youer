package io.papermc.asm.rules.rename;

import io.papermc.asm.util.Builder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;
import java.util.function.Consumer;

import static io.papermc.asm.util.DescriptorUtils.desc;

public interface RenameRuleBuilder extends Builder<RenameRule> {

    //<editor-fold desc="methods" defaultstate="collapsed">
    default RenameRuleBuilder methodByClass(final Set<Class<?>> owners, final String legacyMethodName, final MethodTypeDesc methodDesc, final String newMethodName) {
        for (final Class<?> owner : owners) {
            this.methodByClass(owner, legacyMethodName, methodDesc, newMethodName);
        }
        return this;
    }

    default RenameRuleBuilder methodByClass(final Class<?> owner, final String legacyMethodName, final MethodTypeDesc methodDesc, final String newMethodName) {
        return this.method(desc(owner), legacyMethodName, methodDesc, newMethodName);
    }

    default RenameRuleBuilder method(final Set<ClassDesc> owners, final String legacyMethodName, final MethodTypeDesc methodDesc, final String newMethodName) {
        for (final ClassDesc owner : owners) {
            this.method(owner, legacyMethodName, methodDesc, newMethodName);
        }
        return this;
    }

    RenameRuleBuilder method(ClassDesc owner, String legacyMethodName, MethodTypeDesc methodDesc, final String newMethodName);
    //</editor-fold>

    //<editor-fold desc="fields" defaultstate="collapsed">
    default RenameRuleBuilder fieldByClass(final Set<Class<?>> owners, final String legacyFieldName, final String newFieldName) {
        for (final Class<?> owner : owners) {
            this.fieldByClass(owner, legacyFieldName, newFieldName);
        }
        return this;
    }

    default RenameRuleBuilder fieldByClass(final Class<?> owner, final String legacyFieldName, final String newFieldName) {
        return this.field(desc(owner), legacyFieldName, newFieldName);
    }

    default RenameRuleBuilder field(final Set<ClassDesc> owners, final String legacyFieldName, final String newFieldName) {
        for (final ClassDesc owner : owners) {
            this.field(owner, legacyFieldName, newFieldName);
        }
        return this;
    }

    RenameRuleBuilder field(ClassDesc owner, String legacyFieldName, String newFieldName);
    //</editor-fold>

    /**
     * Note that you also have to remap the method for the annotation attribute.
     */
    default RenameRuleBuilder annotationAttribute(final Class<?> owner, final String legacyName, final String newName) {
        return this.annotationAttribute(desc(owner), legacyName, newName);
    }

    /**
     * Note that you also have to remap the method for the annotation attribute.
     */
    RenameRuleBuilder annotationAttribute(ClassDesc owner, String legacyName, String newName);

    /**
     * Use {@code /} instead of {@code .}.
     */
    default RenameRuleBuilder type(final String legacyType, final Class<?> newType) {
        return this.type(legacyType, desc(newType));
    }

    /**
     * Use {@code /} instead of {@code .}.
     */
    RenameRuleBuilder type(String legacyType, ClassDesc newType);

    default RenameRuleBuilder editEnum(final Class<?> enumType, final Consumer<io.papermc.asm.rules.rename.EnumRenameBuilder> renameBuilder) {
        return this.editEnum(desc(enumType), renameBuilder);
    }

    RenameRuleBuilder editEnum(ClassDesc enumTypeDesc, Consumer<EnumRenameBuilder> renameBuilder);
}
