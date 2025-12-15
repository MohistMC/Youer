package com.mohistmc.launcher.youer.libraries;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Libraries {

    String path;
    String sha256;
    long size;

    public static Libraries from(String line) {
        String[] parts = line.split("\\|");
        return new Libraries(parts[0], parts[1], Long.parseLong(parts[2]));
    }
}