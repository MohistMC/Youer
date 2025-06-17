package io.papermc.reflectionrewriter.proxygenerator;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

/**
 * Generates holder classes for reflection proxy instances.
 */
@DefaultQualifier(NonNull.class)
public final class ProxyGenerator {
    private ProxyGenerator() {
    }

    /**
     * Generate a holder class for the provided reflection proxy implementation.
     *
     * <p>The holder class is constituted of an {@code INSTANCE} field for holding an instance
     * of the reflection proxy implementation, a {@code public static void init(ProxyImplClass proxyImplClassInstance)}
     * method to be called with reflection to initialize the holder, and for every public method on the implementation
     * class and it's parent classes/interfaces, a static copy of the method which invokes the same method on it's
     * {@code INSTANCE}.</p>
     *
     * @param proxyImplementation proxy implementation class
     * @param generatedClassName  name for generated class
     * @return generated class bytes
     */
    public static byte[] generateProxy(final Class<?> proxyImplementation, final String generatedClassName) {
        final Set<Class<?>> parents = findParents(proxyImplementation);
        return generateProxy(
            classReader(proxyImplementation.getName()),
            generatedClassName,
            parents.stream().map(p -> classReader(p.getName())).toArray(ClassReader[]::new)
        );
    }

    public static byte[] generateProxy(
        final ClassReader proxyImplementation,
        final String generatedClassName,
        final ClassReader... parents
    ) {
        // Discover methods we need to generate static proxies for

        // Scan impl class for methods & impl class name
        final DiscoverMethodsVisitor scanImpl = new DiscoverMethodsVisitor(Opcodes.ASM9, null);
        proxyImplementation.accept(scanImpl, ClassReader.SKIP_CODE);
        final Set<MethodInfo> methods = new HashSet<>(scanImpl.methods);
        final String proxy = scanImpl.name;

        // Next, scan the parents
        // This allows indirect implementation to work (i.e. Impl extends AbstractBase; AbstractBase implements ReflectionProxy)
        // as well as default methods
        for (final ClassReader parent : parents) {
            final DiscoverMethodsVisitor scanInterface = new DiscoverMethodsVisitor(Opcodes.ASM9, null);
            parent.accept(scanInterface, ClassReader.SKIP_CODE);
            methods.addAll(scanInterface.methods);
        }

        // Generate our proxy
        final ClassWriter classWriter = new ClassWriter(0);

        // Header
        classWriter.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, null, "java/lang/Object", null);
        // INSTANCE field
        instanceField(classWriter, proxy);
        // Private default constructor
        constructor(classWriter);
        // public static void init(proxyInstance)
        initMethod(classWriter, generatedClassName, proxy);
        // proxy methods
        for (final MethodInfo method : methods) {
            proxyMethod(classWriter, generatedClassName, proxy, method);
        }
        // done
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private static void instanceField(final ClassWriter classWriter, final String proxy) {
        final FieldVisitor fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_STATIC, "INSTANCE", "L" + proxy + ";", null, null);
        fieldVisitor.visitEnd();
    }

    private static void constructor(final ClassWriter classWriter) {
        final MethodVisitor visitor = classWriter.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        visitor.visitTypeInsn(NEW, "java/lang/IllegalStateException");
        visitor.visitInsn(DUP);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false);
        visitor.visitInsn(ATHROW);
        visitor.visitMaxs(2, 1);
        visitor.visitEnd();
    }

    private static void initMethod(final ClassWriter classWriter, final String generatedClassName, final String proxy) {
        final MethodVisitor visitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "init", "(L" + proxy + ";)V", null, null);
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(PUTSTATIC, generatedClassName, "INSTANCE", "L" + proxy + ";");
        visitor.visitInsn(RETURN);
        visitor.visitMaxs(1, 1);
        visitor.visitEnd();
    }

    private static void proxyMethod(final ClassWriter classWriter, final String generatedClassName, final String proxy, final MethodInfo method) {
        final MethodVisitor visitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, method.name, method.descriptor, method.signature, method.exceptions);
        visitor.visitCode();
        visitor.visitFieldInsn(GETSTATIC, generatedClassName, "INSTANCE", "L" + proxy + ";");
        int stackSize = 1;
        final Type methodType = Type.getType(method.descriptor);
        int locals = 0;
        for (final Type argumentType : methodType.getArgumentTypes()) {
            visitor.visitVarInsn(argumentType.getOpcode(ILOAD), locals);
            locals += argumentType.getSize();
        }
        stackSize += locals;
        visitor.visitMethodInsn(INVOKEVIRTUAL, proxy, method.name, method.descriptor, false);
        visitor.visitInsn(methodType.getReturnType().getOpcode(IRETURN));
        stackSize += methodType.getReturnType().getSize();
        visitor.visitMaxs(stackSize, locals);
        visitor.visitEnd();
    }

    private static Set<Class<?>> findParents(final Class<?> clazz) {
        final Set<Class<?>> ret = new HashSet<>();
        findParents(ret, clazz);
        return ret;
    }

    private static void findParents(final Set<Class<?>> ret, final Class<?> clazz) {
        final @Nullable Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            ret.add(superClass);
            findParents(ret, superClass);
        }
        for (final Class<?> iface : clazz.getInterfaces()) {
            ret.add(iface);
            findParents(ret, iface);
        }
    }

    private static ClassReader classReader(final String className) {
        try (final @Nullable InputStream is = ProxyGenerator.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
            Objects.requireNonNull(is, () -> "Class '" + className + "'");
            return new ClassReader(is);
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to read class '" + className + "'", ex);
        }
    }

    private record MethodInfo(
        String name,
        String descriptor,
        @Nullable String signature,
        String @Nullable [] exceptions
    ) {
        // need to override equals and hashcode because of useless array default

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final MethodInfo that = (MethodInfo) o;
            return this.name.equals(that.name)
                && this.descriptor.equals(that.descriptor)
                && Objects.equals(this.signature, that.signature)
                && Arrays.equals(this.exceptions, that.exceptions);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(this.name, this.descriptor, this.signature);
            result = 31 * result + Arrays.hashCode(this.exceptions);
            return result;
        }
    }

    private static final class DiscoverMethodsVisitor extends ClassVisitor {
        private final List<MethodInfo> methods;
        private @MonotonicNonNull String name;

        DiscoverMethodsVisitor(final int api, final @Nullable ClassVisitor visitor) {
            super(api, visitor);
            this.methods = new ArrayList<>();
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            this.name = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            if (!Modifier.isStatic(access) && Modifier.isPublic(access) && !name.equals("<init>")) {
                this.methods.add(new MethodInfo(name, descriptor, signature, exceptions));
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }
}
