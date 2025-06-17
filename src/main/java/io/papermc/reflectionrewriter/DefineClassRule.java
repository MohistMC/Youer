package io.papermc.reflectionrewriter;

import io.papermc.asm.ClassInfo;
import io.papermc.asm.ClassInfoProvider;
import io.papermc.asm.ClassProcessingContext;
import io.papermc.asm.rules.RewriteRule;
import io.papermc.asm.rules.method.MethodRewriteRule;
import io.papermc.asm.rules.method.rewrite.MethodRewrite;
import io.papermc.asm.rules.method.rewrite.SimpleRewrite;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static io.papermc.asm.util.DescriptorUtils.fromOwner;
import static io.papermc.asm.util.DescriptorUtils.toOwner;
import static io.papermc.asm.util.OpcodeUtils.isStatic;
import static io.papermc.asm.util.OpcodeUtils.staticOp;

@DefaultQualifier(NonNull.class)
public final class DefineClassRule implements MethodRewriteRule {
    private static final Set<String> CLASS_LOADER_DESCS = Set.of(
        "([BII)Ljava/lang/Class;",
        "(Ljava/lang/String;[BII)Ljava/lang/Class;",
        "(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;",
        "(Ljava/lang/String;Ljava/nio/ByteBuffer;Ljava/security/ProtectionDomain;)Ljava/lang/Class;"
    );
    private static final Set<String> SECURE_CLASS_LOADER_DESCS = Set.of(
        "(Ljava/lang/String;Ljava/nio/ByteBuffer;Ljava/security/CodeSource;)Ljava/lang/Class;",
        "(Ljava/lang/String;[BIILjava/security/CodeSource;)Ljava/lang/Class;"
    );

    private final ClassDesc proxy;
    private final boolean assumeClassLoader;

    private DefineClassRule(final String proxyClassName, final boolean assumeClassLoader) {
        this.proxy = fromOwner(proxyClassName);
        this.assumeClassLoader = assumeClassLoader;
    }

    @Override
    public boolean shouldProcess(final ClassProcessingContext context, final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface, final boolean isInvokeDynamic) {
        return true; // see comment below
    }

    // We could split this into multiple rules and return false for shouldProcess when the processing class doesn't
    // extend (S)CL. However since the MethodHandles.Lookup portion always needs to run, the actual benefit would
    // be beyond minute (if not actually worse).
    @Override
    public @Nullable MethodRewrite<?> rewrite(
        final ClassProcessingContext context,
        final boolean isInvokeDynamic,
        final int opcode,
        final ClassDesc ownerDesc,
        final String name,
        MethodTypeDesc descriptor,
        final boolean isInterface
    ) {
        final String owner = toOwner(ownerDesc);
        if (!name.equals("defineClass") || isStatic(opcode, isInvokeDynamic)) {
            return null;
        }
        if (owner.equals("java/lang/invoke/MethodHandles$Lookup") && descriptor.descriptorString().equals("([B)Ljava/lang/Class;")) {
            descriptor = descriptor.insertParameterTypes(0, fromOwner("java/lang/invoke/MethodHandles$Lookup"));
            new SimpleRewrite(staticOp(isInvokeDynamic), this.proxy, name, descriptor, false, isInvokeDynamic);
        }
        final @Nullable String superName = context.processingClassSuperClassName();
        if (superName != null) {
            if (CLASS_LOADER_DESCS.contains(descriptor.descriptorString())
                && this.isClassLoader(context.classInfoProvider(), superName)
                && (owner.equals(context.processingClassName()) || this.isClassLoader(context.classInfoProvider(), owner))) {
                return this.classLoaderRewrite(isInvokeDynamic, name, descriptor);
            } else if (SECURE_CLASS_LOADER_DESCS.contains(descriptor.descriptorString())
                && this.isSecureClassLoader(context.classInfoProvider(), superName)
                && (owner.equals(context.processingClassName()) || this.isSecureClassLoader(context.classInfoProvider(), owner))) {
                return this.classLoaderRewrite(isInvokeDynamic, name, descriptor);
            }
        }
        return null;
    }

    private MethodRewrite<?> classLoaderRewrite(final boolean isInvokeDynamic, final String name, MethodTypeDesc descriptor) {
        descriptor = descriptor.insertParameterTypes(0, fromOwner("java/lang/Object"));
        return new SimpleRewrite(staticOp(isInvokeDynamic), this.proxy, name, descriptor, false, isInvokeDynamic);
    }

    private boolean isSecureClassLoader(final ClassInfoProvider classInfoProvider, final String className) {
        return is(classInfoProvider, className, "java/security/SecureClassLoader", this.assumeClassLoader);
    }

    private boolean isClassLoader(final ClassInfoProvider classInfoProvider, final String className) {
        return is(classInfoProvider, className, "java/lang/ClassLoader", this.assumeClassLoader);
    }

    /**
     * Create a rewrite rule for MethodHandles.Lookup#defineClass and (Secure)ClassLoader#defineClass.
     *
     * @param proxyClassName proxy class name
     * @return new rule
     */
    public static RewriteRule create(final String proxyClassName) {
        return create(proxyClassName, false);
    }

    /**
     * Create a rewrite rule for MethodHandles.Lookup#defineClass and (Secure)ClassLoader#defineClass.
     *
     * @param proxyClassName    proxy class name
     * @param assumeClassLoader whether to assume a class is a {@link ClassLoader} if it cannot be determined
     *                          using the {@link ClassInfoProvider}
     * @return new rule
     */
    public static RewriteRule create(final String proxyClassName, final boolean assumeClassLoader) {
        return new DefineClassRule(proxyClassName, assumeClassLoader);
    }

    private static boolean is(final ClassInfoProvider classInfoProvider, final String className, final String checkForName, final boolean assumeClassLoader) {
        if (className.equals(checkForName)) {
            return true;
        }
        final @Nullable ClassInfo info = classInfoProvider.info(className);
        if (info != null) {
            final @Nullable String superName = info.superClassName();
            if (superName != null) {
                return is(classInfoProvider, superName, checkForName, assumeClassLoader);
            } else {
                return false;
            }
        }
        return assumeClassLoader;
    }
}
