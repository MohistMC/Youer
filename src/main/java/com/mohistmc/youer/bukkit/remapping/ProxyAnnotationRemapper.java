package com.mohistmc.youer.bukkit.remapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Translates the mojmap member names held in sparrow-reflection proxy
 * annotations (CraftEngine's net.momirealms.craftengine.proxy.* interfaces) into
 * the names those plugins see once {@link ReflectionHandler} redirects their
 * reflective lookups. Bytecode member references are already remapped by
 * {@link ClassLoaderRemapper}; annotation string values are not, which is what
 * this transformer covers.
 *
 * <p>The plugin facing namespace is derived from the running server itself
 * ({@link ClassLoaderRemapper#tryMapMethodToBukkit(Class, Method)} and
 * {@link ClassLoaderRemapper#tryMapFieldToBukkit(Class, String, Field)}), because
 * methods and fields are mapped independently and neither follows the vanilla
 * proguard mappings. A proxy binding holds when the annotation name, translated by
 * the plugin's own {@link ReobfTiny} based remapper, equals that view, so names
 * already in the view namespace simply pass through.
 *
 * @author Mgazul
 */
public final class ProxyAnnotationRemapper implements PluginTransformer {

    public static final ProxyAnnotationRemapper INSTANCE = new ProxyAnnotationRemapper();

    private static final Logger LOGGER = LogManager.getLogger("Youer");

    /**
     * Which name a proxy was aimed at and which member it landed on is only worth a line of
     * output while someone is comparing the two, so the host keeps it behind the same switch the
     * rest of the core reads and prints it at a level the server actually forwards.
     */
    private static final boolean DEBUG = Boolean.getBoolean("youer.debug");

    private static final String ANNOTATION_SUFFIX = "/reflection/proxy/annotation/";

    private static final String PROXY_DESC = "L" + ANNOTATION_SUFFIX + "ReflectionProxy;";
    private static final String INVOKER_DESC = "L" + ANNOTATION_SUFFIX + "MethodInvoker;";
    private static final String FIELD_GETTER_DESC = "L" + ANNOTATION_SUFFIX + "FieldGetter;";
    private static final String FIELD_SETTER_DESC = "L" + ANNOTATION_SUFFIX + "FieldSetter;";
    private static final String TYPE_DESC = "L" + ANNOTATION_SUFFIX + "Type;";

    private static final String WILDCARD = "*";

    /**
     * A gate expression leaf without an {@code =}, which sparrow-reflection's
     * predicate parser evaluates to {@code false}. Used to deactivate a proxy
     * binding for a member this server simply does not have.
     */
    private static final String MEMBER_ABSENT = "absent_on_youer";

    private record MethodEntry(Class<?>[] params, Class<?> returnType, boolean isStatic, String view) {
    }

    /**
     * The member names a plugin observes on one NMS class, i.e. exactly what
     * ProxyBinder compares annotation names against.
     */
    private record View(Map<String, List<MethodEntry>> methods, Map<String, String> fields) {
    }

    private static final Object UNRESOLVED = new Object();

    // proxy interface internal name -> target class name
    private final Map<String, String> proxyTargets = new ConcurrentHashMap<>();
    // target class name -> plugin facing member view
    private final Map<String, Object> views = new ConcurrentHashMap<>();
    private final Map<String, Boolean> warned = new ConcurrentHashMap<>();
    private volatile ClassLoader probeLoader = ProxyAnnotationRemapper.class.getClassLoader();

    private ProxyAnnotationRemapper() {
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        if (remapper == null) {
            return;
        }
        boolean hasInvoker = false;
        boolean hasFieldAccessor = false;
        for (MethodNode method : node.methods) {
            if (find(method.visibleAnnotations, INVOKER_DESC) != null || find(method.invisibleAnnotations, INVOKER_DESC) != null) {
                hasInvoker = true;
            }
            if (find(method.visibleAnnotations, FIELD_GETTER_DESC) != null || find(method.invisibleAnnotations, FIELD_GETTER_DESC) != null
                    || find(method.visibleAnnotations, FIELD_SETTER_DESC) != null || find(method.invisibleAnnotations, FIELD_SETTER_DESC) != null) {
                hasFieldAccessor = true;
            }
        }
        if (!hasInvoker && !hasFieldAccessor) {
            return;
        }
        String target = proxyTarget(node);
        if (target == null) {
            return;
        }
        View view = view(target, remapper);
        if (view == null) {
            return;
        }
        for (MethodNode method : node.methods) {
            translateInvoker(method, target, view, remapper);
            translateFieldAccessor(method, target, view, FIELD_GETTER_DESC);
            translateFieldAccessor(method, target, view, FIELD_SETTER_DESC);
        }
    }

    private void translateInvoker(MethodNode method, String owner, View view, ClassLoaderRemapper remapper) {
        AnnotationNode invoker = find(method.visibleAnnotations, INVOKER_DESC);
        if (invoker == null) {
            invoker = find(method.invisibleAnnotations, INVOKER_DESC);
        }
        if (invoker == null) {
            return;
        }
        boolean isStatic = booleanValue(invoker, "isStatic");
        List<String> names = stringArrayValue(invoker, "name");
        if (names == null || names.isEmpty()) {
            return;
        }
        int arity = method.desc == null ? 0 : Type.getArgumentTypes(method.desc).length - (isStatic ? 0 : 1);
        String returns = method.desc == null ? null : Type.getReturnType(method.desc).getDescriptor();
        List<Set<String>> paramNames = resolveParameterTypes(method, isStatic);
        List<String> translated = new ArrayList<>(names.size());
        boolean changed = false;
        for (String name : names) {
            String viewName = resolveMethodName(name, arity, isStatic, returns, paramNames, owner, view);
            if (viewName == null) {
                continue;
            }
            if (!name.equals(viewName)) {
                changed = true;
            }
            translated.add(viewName);
        }
        if (translated.isEmpty()) {
            deactivate(invoker, "MethodInvoker", owner, method.name, names);
            return;
        }
        if (changed) {
            setStringArrayValue(invoker, "name", translated);
            if (DEBUG) {
                LOGGER.warn("[PAR] MAP " + owner + "#" + method.name + " " + names + " -> " + translated);
            }
        }
    }

    /**
     * The name a plugin reads this member as, or null when the proxy wants a method the running
     * class does not have. Proxies bind declared methods only, so a name the class declares at
     * some other signature is a name the plugin's own search also comes back empty for - a
     * distribution that patched in an extra overload is the usual reason, and keeping such a
     * binding on would abort the whole proxy interface at class initialisation.
     */
    private String resolveMethodName(String name, int arity, boolean isStatic, String returns, List<Set<String>> paramNames, String owner, View view) {
        List<MethodEntry> candidates = view.methods().get(name);
        // A member that already answers to this name is what a search for that name reaches, so
        // the name carries nothing to translate, and rewriting it would aim the search somewhere
        // else. This is also how a name a plugin holds for a renamed member still lands on it when
        // the running class kept a second member under that very name.
        if (answersTo(view, name, arity, isStatic, returns)) {
            return name;
        }
        if (candidates == null) {
            return null;
        }
        List<MethodEntry> signatureMatches = new ArrayList<>();
        for (MethodEntry candidate : candidates) {
            if (candidate.params().length == arity && candidate.isStatic() == isStatic) {
                signatureMatches.add(candidate);
            }
        }
        if (signatureMatches.isEmpty()) {
            warnOnce(owner + "#" + name, "no declared method with arity " + arity + " and static=" + isStatic);
            return null;
        }
        if (signatureMatches.size() == 1) {
            return signatureMatches.get(0).view();
        }
        List<MethodEntry> typeMatches = new ArrayList<>();
        for (MethodEntry candidate : signatureMatches) {
            if (typesMatch(candidate, paramNames)) {
                typeMatches.add(candidate);
            }
        }
        if (typeMatches.size() == 1) {
            return typeMatches.get(0).view();
        }
        List<MethodEntry> pool = typeMatches.isEmpty() ? signatureMatches : typeMatches;
        String viewName = pool.get(0).view();
        for (MethodEntry candidate : pool) {
            if (!candidate.view().equals(viewName)) {
                warnOnce(owner + "#" + name, "ambiguous overloads, keeping name");
                return name;
            }
        }
        return viewName;
    }

    private boolean typesMatch(MethodEntry candidate, List<Set<String>> paramNames) {
        for (int i = 0; i < candidate.params().length; i++) {
            Set<String> expected = i < paramNames.size() ? paramNames.get(i) : null;
            if (expected == null || expected.contains(WILDCARD)) {
                continue;
            }
            if (!expected.contains(readableName(candidate.params()[i]))) {
                return false;
            }
        }
        return true;
    }

    private boolean answersTo(View view, String name, int arity, boolean isStatic, String returns) {
        for (List<MethodEntry> entries : view.methods().values()) {
            for (MethodEntry entry : entries) {
                if (entry.view().equals(name) && entry.params().length == arity && entry.isStatic() == isStatic
                        && returnsFit(returns, entry.returnType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean returnsFit(String returns, Class<?> type) {
        if (returns == null) {
            return true;
        }
        if (returns.equals("V")) {
            return type == void.class;
        }
        if (type.isPrimitive() || type == void.class) {
            return returns.equals(Type.getDescriptor(type));
        }
        return true;
    }

    private void translateFieldAccessor(MethodNode method, String owner, View view, String descriptor) {
        AnnotationNode accessor = find(method.visibleAnnotations, descriptor);
        if (accessor == null) {
            accessor = find(method.invisibleAnnotations, descriptor);
        }
        if (accessor == null) {
            return;
        }
        List<String> names = stringArrayValue(accessor, "name");
        if (names == null || names.isEmpty()) {
            return;
        }
        List<String> translated = new ArrayList<>(names.size());
        boolean changed = false;
        for (String name : names) {
            if (!view.fields().containsKey(name)) {
                continue;
            }
            String viewName = view.fields().get(name);
            if (!name.equals(viewName)) {
                changed = true;
            }
            translated.add(viewName);
        }
        if (translated.isEmpty()) {
            deactivate(accessor, "FieldAccessor", owner, method.name, names);
            return;
        }
        if (changed) {
            setStringArrayValue(accessor, "name", translated);
        }
    }

    /**
     * The annotated member does not exist on this server (a patch-only member that
     * the gate heuristics assumed present). Switching the gate off makes
     * ProxyBinder skip the binding, exactly like on a server without that patch,
     * instead of aborting plugin loading.
     */
    private void deactivate(AnnotationNode annotation, String kind, String owner, String method, List<String> names) {
        if (booleanValue(annotation, "optional")) {
            return;
        }
        setOrAddStringValue(annotation, "activeIf", MEMBER_ABSENT);
        if (DEBUG) {
            LOGGER.warn("[PAR] SKIP " + kind + " " + owner + "#" + method + " " + names + " (member absent from host)");
        }
    }

    private View view(String owner, ClassLoaderRemapper remapper) {
        Object cached = views.computeIfAbsent(owner, key -> {
            try {
                Class<?> clazz = Class.forName(key, false, remapper.getClassLoader());
                Map<String, List<MethodEntry>> methods = new HashMap<>();
                for (Method method : clazz.getDeclaredMethods()) {
                    methods.computeIfAbsent(method.getName(), k -> new ArrayList<>())
                            .add(new MethodEntry(method.getParameterTypes(), method.getReturnType(),
                                    Modifier.isStatic(method.getModifiers()), remapper.tryMapMethodToBukkit(clazz, method)));
                }
                Map<String, String> fields = new HashMap<>();
                for (Field field : clazz.getDeclaredFields()) {
                    fields.put(field.getName(), remapper.tryMapFieldToBukkit(clazz, field.getName(), field));
                }
                return new View(Map.copyOf(methods), Map.copyOf(fields));
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOGGER.debug("ProxyAnnotationRemapper: " + key + " is not part of this server");
                return UNRESOLVED;
            } catch (Throwable e) {
                LOGGER.warn("ProxyAnnotationRemapper: unable to read the member view of " + key, e);
                return UNRESOLVED;
            }
        });
        return cached instanceof View view ? view : null;
    }

    /**
     * Annotations may carry several candidate class names for one member
     * (version aliases); pick the first one the running server can actually load.
     */
    private String pickLoadable(List<String> names, ClassLoader loader) {
        String first = names.get(0).replace("{}", ".");
        if (names.size() == 1) {
            return first;
        }
        for (String name : names) {
            String dotted = name.replace("{}", ".");
            if (isLoadable(dotted, loader)) {
                return dotted;
            }
        }
        return first;
    }

    private boolean isLoadable(String owner, ClassLoader loader) {
        Object cached = views.get(owner);
        if (cached != null) {
            return cached instanceof View;
        }
        try {
            Class.forName(owner, false, loader);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String readableName(Class<?> clazz) {
        Class<?> component = clazz;
        int dimensions = 0;
        while (component.isArray()) {
            dimensions++;
            component = component.getComponentType();
        }
        return dimensions == 0 ? component.getName() : component.getName() + "[]".repeat(dimensions);
    }

    /**
     * Acceptable runtime type names per proxy parameter; {@code null} or a set
     * holding {@link #WILDCARD} means "any type".
     */
    private List<Set<String>> resolveParameterTypes(MethodNode method, boolean isStatic) {
        Type[] argumentTypes = Type.getArgumentTypes(method.desc);
        List<Set<String>> resolved = new ArrayList<>();
        List<List<AnnotationNode>> parameterAnnotations = new ArrayList<>();
        if (method.visibleParameterAnnotations != null) {
            for (int i = 0; i < method.visibleParameterAnnotations.length; i++) {
                while (parameterAnnotations.size() <= i) {
                    parameterAnnotations.add(new ArrayList<>());
                }
                List<AnnotationNode> list = method.visibleParameterAnnotations[i];
                if (list != null) {
                    parameterAnnotations.get(i).addAll(list);
                }
            }
        }
        if (method.invisibleParameterAnnotations != null) {
            for (int i = 0; i < method.invisibleParameterAnnotations.length; i++) {
                while (parameterAnnotations.size() <= i) {
                    parameterAnnotations.add(new ArrayList<>());
                }
                List<AnnotationNode> list = method.invisibleParameterAnnotations[i];
                if (list != null) {
                    parameterAnnotations.get(i).addAll(list);
                }
            }
        }
        int start = isStatic ? 0 : 1;
        for (int i = start; i < argumentTypes.length; i++) {
            List<AnnotationNode> annotations = i < parameterAnnotations.size() ? parameterAnnotations.get(i) : null;
            AnnotationNode type = annotations == null ? null : find(annotations, TYPE_DESC);
            if (type == null) {
                resolved.add(Set.of(descToName(argumentTypes[i])));
                continue;
            }
            Type clazz = typeValue(type, "clazz");
            if (clazz != null && clazz.getSort() == Type.OBJECT && !"java/lang/Object".equals(clazz.getInternalName())) {
                String target = resolveProxyTarget(clazz.getInternalName(), 0);
                resolved.add(WILDCARD.equals(target) ? Set.of(WILDCARD) : Set.of(target));
                continue;
            }
            List<String> names = stringArrayValue(type, "name");
            if (names != null && !names.isEmpty()) {
                Set<String> accepted = new HashSet<>();
                for (String name : names) {
                    accepted.add(name.replace("{}", "."));
                }
                resolved.add(accepted);
                continue;
            }
            resolved.add(Set.of(descToName(argumentTypes[i])));
        }
        return resolved;
    }

    private static String descToName(Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN -> "boolean";
            case Type.BYTE -> "byte";
            case Type.CHAR -> "char";
            case Type.SHORT -> "short";
            case Type.INT -> "int";
            case Type.FLOAT -> "float";
            case Type.LONG -> "long";
            case Type.DOUBLE -> "double";
            case Type.ARRAY -> descToName(type.getElementType()) + "[]".repeat(type.getDimensions());
            case Type.OBJECT -> {
                String internal = type.getInternalName();
                if ("java/lang/Object".equals(internal)) {
                    yield WILDCARD;
                }
                yield internal.replace('/', '.');
            }
            default -> WILDCARD;
        };
    }

    private String resolveProxyTarget(String internalName, int depth) {
        if (depth > 8) {
            return WILDCARD;
        }
        String cached = proxyTargets.get(internalName);
        if (cached != null) {
            return cached;
        }
        ClassNode node = GlobalClassRepo.INSTANCE.findClass(internalName);
        if (node == null) {
            proxyTargets.put(internalName, WILDCARD);
            return WILDCARD;
        }
        AnnotationNode proxy = find(node.visibleAnnotations, PROXY_DESC);
        if (proxy == null) {
            proxy = find(node.invisibleAnnotations, PROXY_DESC);
        }
        String target = proxy == null ? null : resolveProxyTargetFrom(proxy);
        if (target == null) {
            target = WILDCARD;
        }
        proxyTargets.put(internalName, target);
        return target;
    }

    private String resolveProxyTargetFrom(AnnotationNode proxy) {
        Type clazz = typeValue(proxy, "clazz");
        if (clazz != null && clazz.getSort() == Type.OBJECT && !"java/lang/Object".equals(clazz.getInternalName())) {
            return clazz.getInternalName().replace('/', '.');
        }
        List<String> names = stringArrayValue(proxy, "name");
        if (names != null && !names.isEmpty()) {
            return pickLoadable(names, probeLoader);
        }
        return null;
    }

    private String proxyTarget(ClassNode node) {
        AnnotationNode proxy = find(node.visibleAnnotations, PROXY_DESC);
        if (proxy == null) {
            proxy = find(node.invisibleAnnotations, PROXY_DESC);
        }
        if (proxy == null) {
            return null;
        }
        String target = resolveProxyTargetFrom(proxy);
        if (target != null) {
            proxyTargets.put(node.name, target);
        }
        return target;
    }

    private void warnOnce(String member, String reason) {
        if (DEBUG && warned.putIfAbsent(member + "|" + reason, Boolean.TRUE) == null) {
            LOGGER.warn("ProxyAnnotationRemapper: unable to translate member '" + member + "': " + reason);
        }
    }

    private static AnnotationNode find(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc != null && annotation.desc.endsWith(descriptor.substring(1))) {
                return annotation;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringArrayValue(AnnotationNode annotation, String key) {
        if (annotation.values == null) {
            return null;
        }
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) {
                Object value = annotation.values.get(i + 1);
                return value instanceof List<?> list ? (List<String>) list : null;
            }
        }
        return null;
    }

    private static void setOrAddStringValue(AnnotationNode annotation, String key, String value) {
        if (annotation.values == null) {
            annotation.values = new ArrayList<>(List.of(key, value));
            return;
        }
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) {
                annotation.values.set(i + 1, value);
                return;
            }
        }
        annotation.values.add(key);
        annotation.values.add(value);
    }

    private static void setStringArrayValue(AnnotationNode annotation, String key, List<String> value) {
        if (annotation.values == null) {
            return;
        }
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) {
                annotation.values.set(i + 1, value);
                return;
            }
        }
    }

    private static Type typeValue(AnnotationNode annotation, String key) {
        if (annotation.values == null) {
            return null;
        }
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) {
                Object value = annotation.values.get(i + 1);
                return value instanceof Type t ? t : null;
            }
        }
        return null;
    }

    private static boolean booleanValue(AnnotationNode annotation, String key) {
        if (annotation.values == null) {
            return false;
        }
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) {
                Object value = annotation.values.get(i + 1);
                if (value instanceof Boolean b) {
                    return b;
                }
                return value instanceof Integer i1 && i1 != 0;
            }
        }
        return false;
    }
}
