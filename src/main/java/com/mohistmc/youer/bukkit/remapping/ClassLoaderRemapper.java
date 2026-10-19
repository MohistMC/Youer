package com.mohistmc.youer.bukkit.remapping;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.RemappingClassAdapter;
import net.md_5.specialsource.repo.ClassRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.service.MixinService;

/**
 * ClassLoaderAdapter
 *
 * @author Mainly by IzzelAliz
 * @originalClassName ClassLoaderAdapter
 */
public class ClassLoaderRemapper extends LenientJarRemapper {

    private static final Logger LOGGER = LogManager.getLogger("Youer");
    private static final boolean DEBUG = Boolean.getBoolean("youer.debug");
    private static final String PREFIX = "net/minecraft/";
    private static final String REPLACED_NAME = Type.getInternalName(ReflectionHandler.class);
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final JarMapping toBukkitMapping;
    private final JarRemapper toBukkitRemapper;
    @Getter
    private final ClassLoader classLoader;
    @Getter
    private final String generatedHandler;
    @Getter
    private final Class<?> generatedHandlerClass;
    private final GeneratedHandlerAdapter generatedHandlerAdapter;
    private final Map<String, Boolean> secureJarInfo = new ConcurrentHashMap<>();
    // BiMap: srg -> bukkit
    private final Map<String, BiMap<Field, String>> cacheFields = new ConcurrentHashMap<>();
    private final Map<String, Map.Entry<Map<Method, String>, Map<WrappedMethod, Method>>> cacheMethods = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cacheRemap = new ConcurrentHashMap<>();
    // kept so it can be unregistered from GlobalClassRepo when the plugin is unloaded
    private final ClassLoaderRepo repo;
    private final Set<String> runtimeClasses = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> cacheSearch = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> cacheShared = new ConcurrentHashMap<>();

    public ClassLoaderRemapper(JarMapping jarMapping, JarMapping toBukkitMapping, ClassLoader classLoader) {
        super(jarMapping);
        this.toBukkitMapping = toBukkitMapping;
        this.classLoader = classLoader;
        this.jarMapping.setFallbackInheritanceProvider(GlobalClassRepo.inheritanceProvider());
        this.toBukkitMapping.setFallbackInheritanceProvider(GlobalClassRepo.inheritanceProvider());
        this.toBukkitRemapper = new LenientJarRemapper(this.toBukkitMapping);
        this.generatedHandlerClass = generateReflectionHandler();
        this.generatedHandler = Type.getInternalName(generatedHandlerClass);
        this.generatedHandlerAdapter = new GeneratedHandlerAdapter(REPLACED_NAME, generatedHandler);
        this.repo = new ClassLoaderRepo(this.classLoader);
        GlobalClassRepo.INSTANCE.addRepo(this.repo);
    }

    // drop repo + runtime entries so an unloaded plugin's ClassLoader can be GC'd
    public void dispose() {
        GlobalClassRepo.INSTANCE.removeRepo(this.repo);
        for (String name : runtimeClasses) GlobalClassRepo.runtimeRepo().remove(name);
        runtimeClasses.clear();
    }

    public JarMapping toBukkitMapping() {
        return toBukkitMapping;
    }

    public JarMapping toNmsMapping() {
        return jarMapping;
    }

    public JarRemapper toBukkitRemapper() {
        return toBukkitRemapper;
    }

    private Map.Entry<Map<Method, String>, Map<WrappedMethod, Method>> getMethods(Class<?> cl, String internalName) {
        return cacheMethods.computeIfAbsent(internalName, k -> this.tryGetMethods(cl));
    }

    private Map.Entry<Map<Method, String>, Map<WrappedMethod, Method>> tryGetMethods(Class<?> cl) {
        Map<Method, String> names = new HashMap<>();
        Map<WrappedMethod, Method> types = new HashMap<>();
        try {
            for (Method method : cl.getDeclaredMethods()) {
                collectMethod(names, types, method);
            }

            for (Method method : cl.getMethods()) {
                collectMethod(names, types, method);
            }
            return Maps.immutableEntry(names, types);
        } catch (TypeNotPresentException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                tryDefineClass(e.getCause().getMessage().replace('.', '/'));
                return tryGetMethods(cl);
            } else throw e;
        } catch (NoClassDefFoundError | IncompatibleClassChangeError error) {
            return Maps.immutableEntry(names, types);
        }
    }

    private void collectMethod(Map<Method, String> names, Map<WrappedMethod, Method> types, Method method) {
        try {
            checkMethodTypes(method);
            String name = mapMethod(method);
            names.put(method, name);
            WrappedMethod wrapped = new WrappedMethod(name, method.getParameterTypes());
            types.put(wrapped, method);
        } catch (TypeNotPresentException e) {
            // The caller answers this by defining the missing class and starting over.
            throw e;
        } catch (RuntimeException | Error e) {
            // A single signature that the mod launcher cannot resolve yet must not hide
            // every other member of the class; those are answered from the member table.
            LOGGER.debug("ClassLoaderRemapper: skipped " + method, e);
        }
    }

    private BiMap<Field, String> getFields(Class<?> cl, String internalName) {
        return cacheFields.computeIfAbsent(internalName, k -> this.tryGetFields(cl));
    }

    private BiMap<Field, String> tryGetFields(Class<?> cl) {
        try {
            HashBiMap<Field, String> map = HashBiMap.create();
            for (Field field : cl.getFields()) {
                collectField(map, field);
            }
            for (Field field : cl.getDeclaredFields()) {
                collectField(map, field);
            }
            return map;
        } catch (TypeNotPresentException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                tryDefineClass(e.getCause().getMessage().replace('.', '/'));
                return tryGetFields(cl);
            } else throw e;
        } catch (NoClassDefFoundError error) {
            tryDefineClass(error.getMessage());
            return tryGetFields(cl);
        }
    }

    private void collectField(HashBiMap<Field, String> map, Field field) {
        try {
            checkFieldTypes(field);
            map.forcePut(field, mapField(field));
        } catch (TypeNotPresentException e) {
            throw e;
        } catch (RuntimeException | Error e) {
            LOGGER.debug("ClassLoaderRemapper: skipped " + field, e);
        }
    }

    private void checkFieldTypes(Field field) throws TypeNotPresentException {
        field.getGenericType();
    }

    private void checkMethodTypes(Method method) throws TypeNotPresentException {
        method.getGenericReturnType();
        method.getGenericParameterTypes();
    }

    public void tryDefineClass(String internalName) {
        if (!internalName.startsWith(PREFIX)) {
            throw new NoClassDefFoundError(internalName);
        }
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_DEPRECATED, internalName, null, "java/lang/Object", new String[]{});
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        Unsafe.defineClass(Type.getObjectType(internalName).getClassName(), bytes, 0, bytes.length, getClass().getClassLoader(), getClass().getProtectionDomain());
    }

    private String mapMethod(Method method) {
        String name = tableMethodName(method);
        if (name.equals(method.getName())) {
            String shared = sharedMethodNames(Type.getInternalName(method.getDeclaringClass())).get(method.getName() + paramsDesc(method.getParameterTypes()));
            if (shared != null) {
                return shared;
            }
        }
        return name;
    }

    private String tableMethodName(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        String desc = Type.getMethodDescriptor(method);
        if (!ReobfTiny.INSTANCE.knows(owner)) {
            return toBukkitRemapper.mapMethodName(owner, method.getName(), desc, -1);
        }
        if (!pluginSearchesMembers(owner)) {
            return method.getName();
        }
        String legacy = ReobfTiny.INSTANCE.methodName(owner, method.getName(), desc);
        return legacy == null ? method.getName() : legacy;
    }

    /**
     * The methods a distribution names alike, keyed by that name and its parameters.
     *
     * <p>A plugin finds a method by its name and parameters, so two methods answering to one of
     * those names leave the plugin with whichever one the class happens to report first - and a
     * proxy bound that way gets a body for the other one. The table renames a method onto the
     * name a patch gave a second one when it split a signature apart, which is what a hybrid
     * server has and a plain distribution does not, so the patched one keeps a name of its own.
     *
     * <p>A compiler writes the bridge form of a covariant override beside the method it forwards
     * to, under one name and one parameter list, so those pairs are what every class reports and
     * not a split signature; only the methods a distribution really differs over are named apart.
     */
    private Map<String, String> sharedMethodNames(String owner) {
        return cacheShared.computeIfAbsent(owner, key -> findSharedMethodNames(key));
    }

    private Map<String, String> findSharedMethodNames(String owner) {
        try {
            Class<?> clazz = Class.forName(Type.getObjectType(owner).getClassName(), false, classLoader);
            Map<String, List<Method>> byName = new HashMap<>();
            for (Method method : clazz.getDeclaredMethods()) {
                if (isBridgeMethod(method)) {
                    continue;
                }
                byName.computeIfAbsent(tableMethodName(method) + paramsDesc(method.getParameterTypes()), k -> new ArrayList<>()).add(method);
            }
            Map<String, String> shared = new HashMap<>();
            for (Map.Entry<String, List<Method>> entry : byName.entrySet()) {
                if (entry.getValue().size() < 2) {
                    continue;
                }
                for (Method method : entry.getValue()) {
                    if (method.getName().equals(tableMethodName(method))) {
                        String name = method.getName() + "$" + Type.getReturnType(Type.getMethodDescriptor(method)).getDescriptor();
                        shared.put(method.getName() + paramsDesc(method.getParameterTypes()), name);
                        if (DEBUG) {
                            LOGGER.warn("ClassLoaderRemapper: " + owner + "#" + method.getName() + paramsDesc(method.getParameterTypes())
                                    + " is named alike by " + entry.getValue().size() + " methods, showing it as " + name);
                        }
                    }
                }
            }
            return shared;
        } catch (Throwable e) {
            LOGGER.debug("ClassLoaderRemapper: cannot read the methods of " + owner, e);
            return Map.of();
        }
    }

    private static boolean isBridgeMethod(Method method) {
        return method.isBridge() || method.isSynthetic();
    }

    private String mapField(Field field) {
        String owner = Type.getInternalName(field.getDeclaringClass());
        String desc = Type.getDescriptor(field.getType());
        if (!ReobfTiny.INSTANCE.knows(owner)) {
            return toBukkitRemapper.mapFieldName(owner, field.getName(), desc, -1);
        }
        if (!pluginSearchesMembers(owner)) {
            return field.getName();
        }
        String legacy = ReobfTiny.INSTANCE.fieldName(owner, field.getName());
        return legacy == null ? field.getName() : legacy;
    }

    /**
     * Whether a plugin that translates the member names it searches for resolves members of this
     * class through the table at all.
     *
     * <p>Such a plugin first looks the class up under the name {@link ReflectionHandler} shows it
     * and only reads a member from the class it finds, so a class the plugin cannot find under
     * that name leaves all of its members with the names the running server gives them. The two
     * names only have to agree with each other, not with the running class: whichever one a
     * plugin holds for it, that is the one it searches with.
     */
    private boolean pluginSearchesMembers(String owner) {
        return cacheSearch.computeIfAbsent(owner, key -> {
            String destination = ReobfTiny.INSTANCE.mapClassName(key);
            return destination != null && destination.equals(toBukkitRemapper.mapType(key));
        });
    }

    private static String paramsDesc(Class<?>[] types) {
        StringBuilder builder = new StringBuilder();
        for (Class<?> type : types) {
            builder.append(Type.getDescriptor(type));
        }
        return builder.toString();
    }

    public String tryMapDecFieldToSrg(Class<?> cl, String bukkitName) {
        String internalName = Type.getInternalName(cl);
        if (internalName.startsWith(PREFIX)) {
            Field field = getFields(cl, internalName).inverse().get(bukkitName);
            if (field != null) {
                return field.getName();
            }
            String runtime = ReobfTiny.INSTANCE.runtimeFieldName(cl, bukkitName);
            return runtime == null ? bukkitName : runtime;
        } else return bukkitName;
    }

    public String tryMapFieldToSrg(Class<?> cl, String bukkitName) {
        String internalName = Type.getInternalName(cl);
        if (shouldRemap(internalName)) {
            Field field = getFields(cl, internalName).inverse().get(bukkitName);
            if (field != null) {
                return field.getName();
            }
            String runtime = ReobfTiny.INSTANCE.runtimeFieldName(cl, bukkitName);
            return runtime == null ? bukkitName : runtime;
        } else return bukkitName;
    }

    public String tryMapFieldToBukkit(Class<?> cl, String srgName, Field field) {
        String internalName = Type.getInternalName(cl);
        if (internalName.startsWith(PREFIX)) {
            String name = getFields(cl, internalName).get(field);
            // The cache is built by resolving the type of every member, which a hybrid
            // server cannot always do; those members are still nameable from the table.
            if (name != null) {
                return name;
            }
            try {
                return mapField(field);
            } catch (RuntimeException | Error e) {
                return srgName;
            }
        } else return srgName;
    }

    public Method tryMapMethodToSrg(Class<?> cl, String bukkitName, Class<?>[] pTypes) {
        if (cl == null) return null;
        String internalName = Type.getInternalName(cl);
        if (!shouldRemap(internalName)) return null;
        Method method = getMethods(cl, internalName).getValue().get(new WrappedMethod(bukkitName, pTypes));
        if (method != null) {
            return method;
        }
        // A plugin compiled against spigot asks for the short name that distribution really
        // runs with, while this server holds the Mojang one the table pairs it with.
        String runtime = ReobfTiny.INSTANCE.runtimeMethodName(internalName, bukkitName, paramsDesc(pTypes));
        if (runtime == null) {
            return null;
        }
        try {
            return cl.getMethod(runtime, pTypes);
        } catch (NoSuchMethodException ignored) {
            // fall through to the declared form, which also reaches non public members
        }
        try {
            return cl.getDeclaredMethod(runtime, pTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    public String tryMapMethodToBukkit(Class<?> cl, Method method) {
        String internalName = Type.getInternalName(cl);
        if (shouldRemap(internalName)) {
            Map<Method, String> names = getMethods(cl, internalName).getKey();
            String cached = names.get(method);
            if (cached != null) {
                return cached;
            }
            try {
                return mapMethod(method);
            } catch (RuntimeException | Error e) {
                return method.getName();
            }
        } else {
            return method.getName();
        }
    }

    private boolean shouldRemap(String internalName) {
        Boolean b = cacheRemap.get(internalName);
        if (b != null) return b;
        for (String s : GlobalClassRepo.inheritanceProvider().getAll(internalName)) {
            if (s.startsWith(PREFIX)) {
                cacheRemap.put(internalName, true);
                return true;
            }
        }
        cacheRemap.put(internalName, false);
        return false;
    }

    public MethodInsnNode mapMethod(String owner, String name, String desc) {
        Map.Entry<String, String> entry = tryClimb(jarMapping.methods, owner, name + " " + desc, -1);
        if (entry == null) return null;
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, mapType(entry.getKey()), entry.getValue(), mapMethodDesc(desc), false);
    }

    public Map.Entry<String, String> tryClimb(Map<String, String> map, String owner, String name, int access) {
        String key = owner + "/" + name;

        String mapped = map.get(key);
        if (mapped == null) return null;
        return Maps.immutableEntry(owner, mapped);
    }

    public Product2<byte[], CodeSource> remapClass(String className, Callable<byte[]> byteSource, URLConnection connection) throws ClassNotFoundException {
        try {
            byte[] bytes = remapClassFile(byteSource.call(), GlobalClassRepo.INSTANCE);
            URL url;
            CodeSigner[] signers;
            if (connection instanceof JarURLConnection) {
                url = ((JarURLConnection) connection).getJarFileURL();
                signers = ((JarURLConnection) connection).getJarEntry().getCodeSigners();
            } else {
                url = connection.getURL();
                signers = null;
            }
            return Product.of(bytes, new CodeSource(url, signers));

        } catch (Exception e) {
            throw new ClassNotFoundException(className, e);
        }
    }

    @Override
    public byte[] remapClassFile(byte[] in, ClassRepo repo) {
        return remapClassFile(in, repo, false);
    }

    public byte[] remapClassFile(byte[] in, ClassRepo repo, boolean runtime) {
        if (runtime) runtimeClasses.add(GlobalClassRepo.runtimeRepo().put(in));
        return remapClassFile(new ClassReader(in), repo);
    }

    private byte[] remapClassFile(ClassReader reader, final ClassRepo repo) {
        ClassNode node = new ClassNode();
        RemappingClassAdapter mapper = new RemappingClassAdapter(node, this, repo);
        reader.accept(mapper, 0);

        for (PluginTransformer transformer : Remapper.INSTANCE.getTransformerList()) {
            transformer.handleClass(node, this);
        }

        ClassWriter wr = new PluginClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(wr);

        return wr.toByteArray();
    }

    private Class<?> generateReflectionHandler() {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(Type.getInternalName(ReflectionHandler.class));
            Preconditions.checkNotNull(node, "node");
            ClassWriter writer = new ClassWriter(0);
            String name = Type.getInternalName(ReflectionHandler.class) + "_" + COUNTER.getAndIncrement();
            ClassVisitor visitor = new ClassRemapper(writer, new NameRemapper(name));
            node.accept(visitor);
            byte[] bytes = writer.toByteArray();
            Class<?> cl = Unsafe.defineClass(name.replace('/', '.'), bytes, 0, bytes.length, getClass().getClassLoader(), getClass().getProtectionDomain());
            MethodHandles.lookup().ensureInitialized(cl);

            Field remapper = cl.getField("remapper");
            remapper.set(null, this);
            return cl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class NameRemapper extends org.objectweb.asm.commons.Remapper {

        private static final String ORIGIN = Type.getInternalName(ReflectionHandler.class);

        private final String internal;

        private NameRemapper(String internal) {
            this.internal = internal;
        }

        @Override
        public String map(String internalName) {
            if (internalName.equals(ORIGIN)) {
                return internal;
            }
            return super.map(internalName);
        }
    }

    private static class PluginClassWriter extends ClassWriter {

        public PluginClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            Collection<String> parents = GlobalClassRepo.remappingProvider().getAll(type2);
            if (parents.contains(type1)) {
                return type1;
            }
            if (GlobalClassRepo.remappingProvider().getAll(type1).contains(type2)) {
                return type2;
            }
            do {
                type1 = getSuper(type1);
            } while (!parents.contains(type1));
            return type1;
        }

        private String getSuper(final String typeName) {
            ClassNode node = GlobalClassRepo.INSTANCE.findClass(typeName);
            if (node == null) {
                return "java/lang/Object";
            }
            return Remapper.getNmsMapper().map(node.superName);
        }
    }

    private static class GeneratedHandlerAdapter extends org.objectweb.asm.commons.Remapper {

        private final String from, to;

        private GeneratedHandlerAdapter(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String map(String internalName) {
            if (from.equals(internalName)) {
                return to;
            } else {
                return internalName;
            }
        }
    }

    record WrappedMethod(String name, Class<?>[] pTypes) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrappedMethod that = (WrappedMethod) o;
            return Objects.equals(name, that.name) &&
                    Arrays.equals(pTypes, that.pTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(name);
            result = 31 * result + Arrays.hashCode(pTypes);
            return result;
        }
    }
}
