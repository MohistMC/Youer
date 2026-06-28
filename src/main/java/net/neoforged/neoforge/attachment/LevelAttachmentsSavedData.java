/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@ApiStatus.Internal
public class LevelAttachmentsSavedData extends SavedData {
    public static final SavedDataType<LevelAttachmentsSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(NeoForgeMod.MOD_ID, "data_attachments"),
            LevelAttachmentsSavedData::new,
            LevelAttachmentsSavedData::makeCodec);

    public static void init(ServerLevel level) {
        // Querying the attachment a single time is enough to initialize it,
        // and make sure it gets saved when the level is saved.
        var data = level.getDataStorage().computeIfAbsent(TYPE);
        if (data.level == null) {
            data.level = level;
        }
    }

    private static Codec<LevelAttachmentsSavedData> makeCodec(@Nullable ServerLevel level) {
        return CompoundTag.CODEC.flatXmap(tag -> {
            var data = new LevelAttachmentsSavedData(level);
            if (level != null) {
                ProblemReporter.Collector reporter = new ProblemReporter.Collector();
                // Note: Side effect here, keep an eye on this
                data.level.deserializeAttachments(TagValueInput.create(reporter, data.level.registryAccess(), tag));
                if (!reporter.isEmpty()) {
                    return DataResult.error(() -> "Deserialisation error in level attachments: " + reporter.getReport());
                }
            }
            return DataResult.success(data);
        }, data -> {
            if (data.level != null) {
                ProblemReporter.Collector reporter = new ProblemReporter.Collector();
                var tag = TagValueOutput.createWithContext(reporter, data.level.registryAccess());
                data.level.serializeAttachments(tag);
                if (!reporter.isEmpty()) {
                    return DataResult.error(() -> "Serialisation error in level attachments: " + reporter.getReport());
                }
                return DataResult.success(tag.buildResult());
            }
            return DataResult.success(new CompoundTag());
        });
    }

    private @Nullable ServerLevel level;

    public LevelAttachmentsSavedData(@Nullable ServerLevel level) {
        this.level = level;
    }

    @Override
    public boolean isDirty() {
        return this.level != null;
    }
}
