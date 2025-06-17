package com.mohistmc.youer.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

/**
 * Implementer
 *
 * @author Mainly by IzzelAliz
 * @originalClassName Implementer
 */
public interface Implementer {

    Logger LOGGER = LogManager.getLogger("Implementer");

    boolean processClass(ClassNode node);
}
