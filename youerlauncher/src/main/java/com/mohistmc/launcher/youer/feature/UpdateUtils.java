/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2025.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.launcher.youer.feature;

import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.util.DataParser;
import com.mohistmc.launcher.youer.util.I18n;
import com.mohistmc.mjson.Json;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateUtils {

    public static final String API = "https://api.mohistmc.com";
    public static final String API_CN = "https://api.mohistmc.cn";

    public static void versionCheck() {
        System.out.println(I18n.as("update.check"));
        System.out.println(I18n.as("update.stopcheck"));

        try {
            String api = YouerConfigUtil.isCN() ? API_CN : API;
            Json json = Json.read(URI.create(api + "/project/youer/1.21.1/builds/latest").toURL());
            String localCommitHash = DataParser.versionMap.get("youer");
            String remoteCommitHash = json.at("commit").asString("hash");
            String remoteCommitHashShort = remoteCommitHash.substring(0, Math.min(8, remoteCommitHash.length()));

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(Date.from(java.time.Instant.parse(json.asString("build_date"))));

            if (localCommitHash.equals(remoteCommitHashShort)) {
                System.out.println(I18n.as("update.latest", localCommitHash, remoteCommitHashShort));
            } else {
                System.out.println(I18n.as("update.detect", remoteCommitHashShort, localCommitHash, time));
            }
        } catch (Throwable e) {
            System.out.println(I18n.as("check.update.noci"));
        }
    }
}
