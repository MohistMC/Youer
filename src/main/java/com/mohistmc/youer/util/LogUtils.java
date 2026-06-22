package com.mohistmc.youer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mgazul
 * @date 2026/6/22 21:10
 */
public class LogUtils {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // Paper start
    public static Logger getClassLogger() {
        return LoggerFactory.getLogger(STACK_WALKER.getCallerClass().getSimpleName());
    }
    // Paper end
}
