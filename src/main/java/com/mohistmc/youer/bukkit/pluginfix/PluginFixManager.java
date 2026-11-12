package com.mohistmc.youer.bukkit.pluginfix;

import com.mohistmc.youer.Youer;
import com.mohistmc.youer.YouerConfig;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.ARETURN;

public class PluginFixManager {

    private static final String WE_PROPERTY_SKIP_MARKER = "__youer_modprop_skip__:";

    public static byte[] injectPluginFix(String plugin, String className, byte[] clazz) {
        if (plugin.equals("WorldEdit")) {
            String adapter = System.getProperty("worldedit.bukkit.adapter");
            if (adapter == null) {
                System.setProperty("worldedit.bukkit.adapter", "com.sk89q.worldedit.bukkit.adapter.impl.v1_21.PaperweightAdapter");
            }
        }
        if (className.startsWith("com.sk89q.worldedit") && className.contains("PaperweightAdapter")) {
            return patch(clazz, node -> {
                helloWorld(node, "org.spigotmc.WatchdogThread", Youer.modid);
                for (MethodNode m : node.methods) {
                    paperweightLoad(m);
                    paperweightGetProps(m);
                }
            });
        }
        switch (className) {
            case "com.ghostchu.quickshop.platform.spigot.AbstractSpigotPlatform" -> {
                return patch(clazz, PluginFixManager::qs);
            }
            case "com.fastasyncworldedit.bukkit.util.MinecraftVersion" -> {
                return patch(clazz, PluginFixManager::fawe);
            }
            case "com.bgsoftware.superiorskyblock.external.ProvidersManagerImpl" -> {
                return patch(clazz, PluginFixManager::removePaper);
            }
            case "com.onarandombox.MultiverseCore.utils.WorldManager" -> {
                return patch(clazz, MultiverseCore::fix);
            }
            case "net.Zrips.CMILib.Version.Version" -> {
                return patch(clazz, PluginFixManager::cmilib);
            }
            case "dev.geco.gsit.GSitMain" -> {
                return patch(clazz, PluginFixManager::gsit);
            }
        }
        if (className.startsWith("net.Zrips.CMILib.") || className.startsWith("com.Zrips.CMI.")) {
            return patch(clazz, node -> helloWorld(node, "net.minecraft.server.network.PlayerConnection", "net.minecraft.server.network.ServerGamePacketListenerImpl"));
        }
        if (className.endsWith(".io.github.classgraph.ModuleReaderProxy")) {
            return patch(clazz, PluginFixManager::fixClassGraph);
        }
        Consumer<ClassNode> patcher = switch (className) {
            case "com.earth2me.essentials.utils.VersionUtil" -> node -> {
                helloWorld(node, "brand:", "peace");
                ex(node);
            };
            case "net.Zrips.CMILib.Reflections" -> node -> helloWorld(node, "bR", "f_36096_");
            case "net.Zrips.CMILib.RawMessages.RawMessageManager" ->
                    node -> helloWorld(node, "net.minecraft.server.network.PlayerConnection", "net.minecraft.server.network.ServerGamePacketListenerImpl");
            case "com.sk89q.worldedit.bukkit.BukkitConfiguration" -> node -> {
                helloWorld(node, "I accept that I will receive no support with this flag enabled.", Youer.modid);
                helloWorld(node, "allow-editing-on-unsupported-versions", Youer.modid);
                helloWorld(node, "false", Youer.modid);
            };
            case "cn.lunadeer.dominion.utils.Misc" ->
                    node -> helloWorld(node, "io.papermc.paper.threadedregions.scheduler.ScheduledTask", Youer.modid);
            case "com.sk89q.worldedit.bukkit.paperlib.PaperLib" -> node -> {
                removePaper0(node);
                String adapter = System.getProperty("paperlib.shown-benefits");
                if (adapter == null) {
                    System.setProperty("paperlib.shown-benefits", "1");
                }
            };
            case "org.mvplugins.multiverse.external.paperlib.PaperLib",
                 "me.SuperRonanCraft.BetterRTP.lib.paperlib.PaperLib", "com.plotsquared.bukkit.paperlib.PaperLib" ->
                    PluginFixManager::removePaper0;
            default -> null;
        };

        return patcher == null ? clazz : patch(clazz, patcher);
    }

    private static void removePaper(ClassNode node) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals("hasPaperAsyncSupport") && methodNode.desc.equals("()Z")) {
                InsnList toInject = new InsnList();
                toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(PluginFixManager.class), "hasPaperAsyncSupport", "()Z"));
                toInject.add(new InsnNode(Opcodes.IRETURN));
                methodNode.instructions = toInject;
            }
        }
    }

    private static void cmilib(ClassNode node) {
        if (!YouerConfig.custom_fix_cmi_tempban) return;
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals("isPaperBranch") && methodNode.desc.equals("()Z")) {
                InsnList toInject = new InsnList();
                toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(PluginFixManager.class), "isPaperBranch", "()Z"));
                toInject.add(new InsnNode(Opcodes.IRETURN));
                methodNode.instructions = toInject;
            }
        }
    }

    private static void gsit(ClassNode node) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals("isPaperServer") && methodNode.desc.equals("()Z")) {
                InsnList toInject = new InsnList();
                toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(PluginFixManager.class), "isPaperServer", "()Z"));
                toInject.add(new InsnNode(Opcodes.IRETURN));
                methodNode.instructions = toInject;
            }
        }
    }

    private static void removePaper0(ClassNode node) {
        helloWorld(node, "com.destroystokyo.paper.PaperConfig", Youer.modid);
        helloWorld(node, "io.papermc.paper.configuration.Configuration", Youer.modid);
    }

    public static boolean hasPaperAsyncSupport() {
        return false;
    }

    // GSit
    public static boolean isPaperServer() {
        return false;
    }

    private static byte[] patch(byte[] basicClass, Consumer<ClassNode> handler) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        handler.accept(node);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void redirectMethodToGetNMSVersion(ClassNode node, String methodName) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals(methodName) && methodNode.desc.equals("()Ljava/lang/String;")) {
                InsnList toInject = new InsnList();
                toInject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(PluginFixManager.class),
                        "getNMSVersion",
                        "()Ljava/lang/String;"
                ));
                toInject.add(new InsnNode(Opcodes.ARETURN));
                methodNode.instructions = toInject;
                methodNode.tryCatchBlocks.clear();
            }
        }
    }

    public static boolean isPaperBranch() {
        return true;
    }

    private static void qs(ClassNode node) {
        redirectMethodToGetNMSVersion(node, "getNMSVersion");
    }

    private static void fawe(ClassNode node) {
        redirectMethodToGetNMSVersion(node, "getPackageVersion");
    }

    // WorldEdit's PaperweightAdapter throws on modded Property subclasses (e.g. Vinery GrapeProperty).
    // Replace the throw with a sentinel WorldEdit EnumProperty so the property cache never blows up,
    // and let getProperties skip it (see putWeProperty).
    private static void paperweightLoad(MethodNode methodNode) {
        if (!methodNode.name.equals("load")) return;
        if (!methodNode.desc.contains("block/state/properties/Property")) return;
        if (!methodNode.desc.contains("registry/state/Property")) return;
        AbstractInsnNode athrow = null;
        for (AbstractInsnNode n = methodNode.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.ATHROW) athrow = n;
        }
        if (athrow == null) return;
        AbstractInsnNode lastInstanceof = null;
        for (AbstractInsnNode n = methodNode.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.INSTANCEOF) lastInstanceof = n;
            if (n == athrow) break;
        }
        if (lastInstanceof == null) return;
        AbstractInsnNode jump = lastInstanceof.getNext();
        while (jump != null && !(jump instanceof JumpInsnNode)) jump = jump.getNext();
        if (!(jump instanceof JumpInsnNode j)) return;
        AbstractInsnNode labelNode = null;
        for (AbstractInsnNode n = methodNode.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n == j.label) { labelNode = n; break; }
        }
        if (labelNode == null) return;
        AbstractInsnNode start = labelNode.getNext();
        if (start == null) return;
        InsnList inject = new InsnList();
        inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/mohistmc/youer/bukkit/pluginfix/PluginFixManager",
                "fixModProperty", "(Ljava/lang/Object;)Ljava/lang/Object;"));
        inject.add(new TypeInsnNode(Opcodes.CHECKCAST, "com/sk89q/worldedit/registry/state/Property"));
        inject.add(new InsnNode(Opcodes.ARETURN));
        methodNode.instructions.insertBefore(start, inject);
        AbstractInsnNode e = start;
        while (e != null) {
            AbstractInsnNode nxt = e.getNext();
            methodNode.instructions.remove(e);
            if (e == athrow) break;
            e = nxt;
        }
    }

    private static void paperweightGetProps(MethodNode methodNode) {
        if (!methodNode.name.equals("getProperties")) return;
        boolean hasCache = false;
        for (AbstractInsnNode n = methodNode.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.GETSTATIC && ((FieldInsnNode) n).name.equals("PROPERTY_CACHE")) {
                hasCache = true;
                break;
            }
        }
        if (!hasCache) return;
        for (AbstractInsnNode n = methodNode.instructions.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.INVOKEINTERFACE && ((MethodInsnNode) n).name.equals("put")
                    && ((MethodInsnNode) n).owner.equals("java/util/Map")) {
                MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "com/mohistmc/youer/bukkit/pluginfix/PluginFixManager",
                        "putWeProperty", "(Ljava/util/Map;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false);
                methodNode.instructions.set(n, replacement);
                return;
            }
        }
    }

    public static Object fixModProperty(Object state) {
        try {
            String realName = String.valueOf(state.getClass().getMethod("getName").invoke(state));
            Class<?> enumProp = loadWeClass("com.sk89q.worldedit.registry.state.EnumProperty", state.getClass().getClassLoader());
            return enumProp.getConstructor(String.class, List.class)
                    .newInstance(WE_PROPERTY_SKIP_MARKER + realName, List.of("__skip__"));
        } catch (Throwable t) {
            try {
                Class<?> enumProp = loadWeClass("com.sk89q.worldedit.registry.state.EnumProperty", state.getClass().getClassLoader());
                return enumProp.getConstructor(String.class, List.class)
                        .newInstance(WE_PROPERTY_SKIP_MARKER + "unknown", List.of("__skip__"));
            } catch (Throwable t2) {
                throw new RuntimeException("Youer: unable to wrap modded WE property " + state.getClass().getName(), t2);
            }
        }
    }

    public static Object putWeProperty(Map<?, ?> map, String name, Object property) {
        if (isSkippedProperty(property)) return null;
        return ((Map<Object, Object>) map).put(name, property);
    }

    public static boolean isSkippedProperty(Object property) {
        if (property == null) return true;
        try {
            Object name = property.getClass().getMethod("getName").invoke(property);
            return name instanceof String s && s.startsWith(WE_PROPERTY_SKIP_MARKER);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Class<?> loadWeClass(String name, ClassLoader fallback) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (ClassLoader loader : new ClassLoader[]{cl, fallback, PluginFixManager.class.getClassLoader()}) {
            if (loader == null) continue;
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last != null ? last : new ClassNotFoundException(name);
    }

    public static void ex(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (method.name.equals("make") && method.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
                InsnList toInject = new InsnList();
                toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        Type.getInternalName(PluginFixManager.class),
                        "make",
                        "(Ljava/lang/String;)Ljava/lang/String;"));
                toInject.add(new InsnNode(Opcodes.ARETURN));
                method.instructions = toInject;
                method.tryCatchBlocks.clear();
            }
        }
    }

    private static void replaceReturn(ClassNode node, String methodName, Object idc) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals(methodName)) {
                InsnList toInject = new InsnList();

                toInject.add(new LdcInsnNode(idc));
                toInject.add(new InsnNode(ARETURN));

                methodNode.instructions = toInject;
                methodNode.tryCatchBlocks.clear();
            }
        }
    }

    public static String getNMSVersion() {
        return "v1_21_R1";
    }

    public static String make(String in) {
        if (in.equals("8(;4>`")) {
            return "peace";
        }
        final char[] c = in.toCharArray();
        for (int i = 0; i < c.length; ++i) {
            c[i] ^= 'Z';
        }
        return new String(c);
    }


    private static void helloWorld(ClassNode node, String a, String b) {
        node.methods.forEach(method -> {
            for (AbstractInsnNode next : method.instructions) {
                if (next instanceof LdcInsnNode ldcInsnNode) {
                    if (ldcInsnNode.cst instanceof String str) {
                        if (a.equals(str)) {
                            ldcInsnNode.cst = b;
                        }
                    }
                }
            }
        });
    }

    private static void helloWorld(ClassNode node, int a, int b) {
        node.methods.forEach(method -> {
            for (AbstractInsnNode next : method.instructions) {
                if (next instanceof IntInsnNode ldcInsnNode) {
                    if (ldcInsnNode.operand == a) {
                        ldcInsnNode.operand = b;
                    }
                }
            }
        });
    }

    private static void fixClassGraph(ClassNode node) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals("list") && methodNode.desc.equals("()Ljava/util/List;")) {
                methodNode.instructions.clear();
                methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false));
                methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
                methodNode.tryCatchBlocks.clear();
                methodNode.localVariables = null;
            }
        }
    }
}
