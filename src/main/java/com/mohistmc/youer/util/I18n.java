package com.mohistmc.youer.util;

import com.mohistmc.youer.Youer;

/**
 * @author Mgazul by MohistMC
 * @date 2023/9/23 6:15:26
 */
public class I18n {

    public static String as(String key) {
        return Youer.i18n.as(key);
    }

    public static String as(String key, Object... objects) {
        return Youer.i18n.as(key, objects);
    }
}
