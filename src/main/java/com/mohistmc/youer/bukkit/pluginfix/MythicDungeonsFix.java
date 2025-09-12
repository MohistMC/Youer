package com.mohistmc.youer.bukkit.pluginfix;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * MythicDungeons 2.0.1 - sendToCheckpoint world/teleport guard fixer
 *
 * Bu class MythicPlayer (net.playavalon.mythicdungeons.player.MythicPlayer) içindeki
 * sendToCheckpoint() metoduna enjekte edilmek üzere yazıldı.
 */
public class MythicDungeonsFix {

    /**
     * ASM ile MythicPlayer.sendToCheckpoint() içinde geçen teleport(...) çağrılarından
     * hemen önce çalışacak şekilde Location argümanını oluşturur:
     * ensureRespawn(this.dungeonRespawn, this.instance)
     *
     * @param node dekompile/classnode
     */
    public static void fix(ClassNode node) {
        // hedef sınıf ismi: net/playavalon/mythicdungeons/player/MythicPlayer
        List<MethodNode> methods = node.methods;
        for (MethodNode method : methods) {
            if ("sendToCheckpoint".equals(method.name) && "()V".equals(method.desc)) {
                // iterate over instructions to find teleport(...) method invocations
                for (AbstractInsnNode ins = method.instructions.getFirst(); ins != null; ins = ins.getNext()) {
                    if (ins instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ins;
                        // hedef teleport çağrıları; iki varyant olabilir: teleport(Location) veya teleport(Location, TeleportFlag...)
                        if ("teleport".equals(min.name) && min.owner.contains("org/bukkit")) {
                            // Öncesindeki (stack'e koyan) eski loading sequence'i temizleyip
                            // bizim static helper'ı çağıracak kodu ekleyeceğiz.

                            // Eğer bir önceki insn GETFIELD dungeonRespawn ise onu ve onun öncesindeki ALOAD 0'ı kaldır
                            AbstractInsnNode before = min.getPrevious();
                            if (before != null && before.getType() == AbstractInsnNode.FIELD_INSN) {
                                FieldInsnNode fin = (FieldInsnNode) before;
                                if ("dungeonRespawn".equals(fin.name)) {
                                    // remove GETFIELD and its previous ALOAD 0 (should be VarInsnNode ALOAD 0)
                                    AbstractInsnNode maybeLoadThis = before.getPrevious();
                                    if (maybeLoadThis != null && maybeLoadThis.getOpcode() == Opcodes.ALOAD) {
                                        method.instructions.remove(maybeLoadThis);
                                    }
                                    method.instructions.remove(before);
                                }
                            }

                            // Insert call to: MythicDungeonsFix.ensureRespawn(this.dungeonRespawn, this.instance)
                            InsnList toInject = new InsnList();
                            // load 'this'
                            toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            // getfield dungeonRespawn : Lorg/bukkit/Location;
                            toInject.add(new FieldInsnNode(Opcodes.GETFIELD,
                                    node.name, // sınıf ismi (internal)
                                    "dungeonRespawn",
                                    "Lorg/bukkit/Location;"));
                            // load 'this'
                            toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            // getfield instance : Lnet/playavalon/mythicdungeons/api/parents/instances/AbstractInstance;
                            toInject.add(new FieldInsnNode(Opcodes.GETFIELD,
                                    node.name,
                                    "instance",
                                    "Lnet/playavalon/mythicdungeons/api/parents/instances/AbstractInstance;"));
                            // invokestatic ensureRespawn
                            toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    Type.getInternalName(MythicDungeonsFix.class),
                                    "ensureRespawn",
                                    "(Lorg/bukkit/Location;Lnet/playavalon/mythicdungeons/api/parents/instances/AbstractInstance;)Lorg/bukkit/Location;",
                                    false));
                            // now stack has Location returned -> teleport will consume it

                            method.instructions.insertBefore(min, toInject);
                        }
                    }
                }
            }
        }
    }

    /**
     * Runtime helper (çağrıldığında Bukkit API kullanacak).
     * - Eğer respawn == null -> return null (bunu çağıran teleport yine hata atacak ama guard'lı kod ayrıca kontrol edilebilir).
     * - Eğer respawn.getWorld() != null -> return respawn
     * - Eğer null ise, instance instanceof DungeonClassic ise getWorldName() alıp Bukkit.getWorld(...) ile yükle / createWorld yap
     * - Eğer world bulunursa respawn.setWorld(world) yapıp respawn'ı döndür.
     */
    public static Location ensureRespawn(Location respawn, Object instance) {
        if (respawn == null) return null;
        if (respawn.getWorld() != null) return respawn;

        try {
            if (instance != null) {
                // sadece class ismine bakıyoruz (string olarak)
                String clazz = instance.getClass().getName();
                if ("net.playavalon.mythicdungeons.dungeons.dungeontypes.DungeonClassic".equals(clazz)) {
                    String worldName = null;
                    try {
                        java.lang.reflect.Method m = instance.getClass().getMethod("getWorldName");
                        Object res = m.invoke(instance);
                        if (res != null) worldName = res.toString();
                    } catch (Throwable ignored) {}

                    if (worldName != null) {
                        World w = Bukkit.getWorld(worldName);
                        if (w == null) {
                            try {
                                w = Bukkit.createWorld(new WorldCreator(worldName));
                            } catch (Throwable ignored) {}
                        }
                        if (w != null) {
                            respawn.setWorld(w);
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            MythicDungeonsFixLog.warn("ensureRespawn encountered an error: " + ex.getMessage());
        }
        return respawn;
    }
}

/**
 * Küçük yardımcı logger (opsiyonel) - eğer proje içinde logger yoksa hata patlamasın diye minimal
 */
class MythicDungeonsFixLog {
    static void warn(String s) {
        try {
            if (Bukkit.getPluginManager() != null) {
                // server log
                Bukkit.getLogger().warning("[MythicFix] " + s);
            }
        } catch (Throwable ignored) {}
    }
}
