package io.papermc.asm.rules.field;

import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.builder.matcher.field.FieldMatcher;
import io.papermc.asm.versioned.ApiVersion;
import io.papermc.asm.versioned.VersionedRuleFactory;
import io.papermc.asm.versioned.matcher.VersionedMatcher;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites a field on matching owners to a method call on the same owner.
 *
 * @param owners the owners to target
 * @param fieldMatcher the field matcher
 * @param getterName the name of the getter method, or null if no getter should be generated
 * @param setterName the name of the setter method, or null if no setter should be generated
 * @param isInterfaceMethod if the method should be marked as an interface method
 */
public record FieldToMethodRewrite(Set<ClassDesc> owners, FieldMatcher fieldMatcher, @Nullable String getterName, @Nullable String setterName, boolean isInterfaceMethod) implements FilteredFieldRewriteRule {

    /**
     * Rewrites a field on matching owners to a method call on the same owner.
     *
     * @param owners the owners to target
     * @param fieldMatcher the field matcher
     * @param getterName the name of the getter method, or null if no getter should be generated
     * @param setterName the name of the setter method, or null if no setter should be generated
     * @param isInterfaceMethod if the method should be marked as an interface method
     * @throws IllegalArgumentException if both getterName and setterName are null
     */
    public FieldToMethodRewrite {
        if (getterName == null && setterName == null) {
            throw new IllegalArgumentException("At least one of getterName or setterName must be non-null");
        }
    }

    private enum Type {
        GETTER {
            @Override
            int opcode(final int fieldOpcode) {
                return switch (fieldOpcode) {
                    case Opcodes.GETFIELD -> Opcodes.INVOKEVIRTUAL;
                    case Opcodes.GETSTATIC -> Opcodes.INVOKESTATIC;
                    default -> throw new IllegalArgumentException("Unexpected opcode: " + fieldOpcode);
                };
            }

            @Override
            MethodTypeDesc desc(final ClassDesc fieldTypeDesc) {
                return MethodTypeDesc.of(fieldTypeDesc);
            }
        },
        SETTER {
            @Override
            int opcode(final int fieldOpcode) {
                return switch (fieldOpcode) {
                    case Opcodes.PUTFIELD -> Opcodes.INVOKEVIRTUAL;
                    case Opcodes.PUTSTATIC -> Opcodes.INVOKESTATIC;
                    default -> throw new IllegalArgumentException("Unexpected opcode: " + fieldOpcode);
                };
            }

            @Override
            MethodTypeDesc desc(final ClassDesc fieldTypeDesc) {
                return MethodTypeDesc.of(ConstantDescs.CD_void, fieldTypeDesc);
            }
        };

        abstract int opcode(final int fieldOpcode);

        abstract MethodTypeDesc desc(final ClassDesc fieldTypeDesc);
    }

    @Override
    public @Nullable Rewrite rewrite(final ClassProcessingContext context, final int opcode, final String owner, final String name, final ClassDesc fieldTypeDesc) {
        final Type type = switch (opcode) {
            case Opcodes.GETFIELD, Opcodes.GETSTATIC -> Type.GETTER;
            case Opcodes.PUTFIELD, Opcodes.PUTSTATIC -> Type.SETTER;
            default -> throw new IllegalArgumentException("Unexpected opcode: " + opcode);
        };
        final @Nullable String methodName = switch (type) {
            case GETTER -> this.getterName;
            case SETTER -> this.setterName;
        };
        if (methodName == null) {
            return null;
        }
        return (delegate) -> {
            delegate.visitMethodInsn(type.opcode(opcode), owner, methodName, type.desc(fieldTypeDesc).descriptorString(), this.isInterfaceMethod);
        };
    }

    public record Versioned(Set<ClassDesc> owners, @Nullable String getterName, @Nullable String setterName, boolean isInterfaceMethod, VersionedMatcher<FieldMatcher> versions) implements VersionedRuleFactory {

        public Versioned {
            if (getterName == null && setterName == null) {
                throw new IllegalArgumentException("At least one of getterName or setterName must be non-null");
            }
        }

        @Override
        public RewriteRule createRule(final ApiVersion<?> apiVersion) {
            return this.versions.ruleForVersion(apiVersion, matcher -> new FieldToMethodRewrite(this.owners(), matcher, this.getterName(), this.setterName(), this.isInterfaceMethod()));
        }
    }
}
