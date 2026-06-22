package com.mohistmc.youer.util;

import java.util.Map;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/18 11:47:11
 */
public record VersionInfo(String youer, String paper, String neoforge) {

    public VersionInfo(Map<String, String> arguments) {
        this(arguments.get("youer"), arguments.get("paper"), arguments.get("neoforge"));
    }

}
