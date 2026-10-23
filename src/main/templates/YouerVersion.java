package com.mohistmc.youer.util;

/**
 * @author Mgazul
 * @date 2026/4/12 02:24
 */
public class YouerVersion {

    public static String getVersion() {
        return "${youer_version}";
    }

    public static String getPaperVersion() {
        return "${paper_version}";
    }

    public static String getPurpurVersion() {
        return "${purpur_version}";
    }
}
