package com.mohistmc.youer.bukkit;

import com.mohistmc.youer.Youer;

/**
 * @author Mgazul by MohistMC
 * @date 2023/10/8 19:16:34
 */
public enum PluginsLibrarySource {

    ALIBABA("https://maven.aliyun.com/repository/public/"),
    MAVEN2("https://repo.maven.apache.org/maven2/");

    public final String url;

    public static final String DEFAULT = Youer.i18n.isCN() ? ALIBABA.url : MAVEN2.url;

    PluginsLibrarySource(String url) {
        this.url = url;
    }
}