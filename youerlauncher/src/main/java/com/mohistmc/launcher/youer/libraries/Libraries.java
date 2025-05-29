package com.mohistmc.launcher.youer.libraries;

import com.mohistmc.tools.SHA256;
import java.io.File;
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

    public static Libraries from(File file) {
        return new Libraries(file.getAbsolutePath(), SHA256.as(file), file.length());
    }
}