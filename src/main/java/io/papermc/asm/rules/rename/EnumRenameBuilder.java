package io.papermc.asm.rules.rename;

import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.papermc.asm.util.DescriptorUtils.desc;

public final class EnumRenameBuilder {

    private final ClassDesc enumTypeDesc;
    private @Nullable ClassDesc alternateValueOfOwner;
    private final Map<String, String> enumFieldRenames = new HashMap<>();

    EnumRenameBuilder(final ClassDesc enumTypeDesc) {
        this.enumTypeDesc = enumTypeDesc;
    }

    public EnumRenameBuilder alternateValueOfOwner(final Class<?> type) {
        return this.alternateValueOfOwner(desc(type));
    }

    public EnumRenameBuilder alternateValueOfOwner(final ClassDesc type) {
        if (this.enumTypeDesc.equals(type)) {
            throw new IllegalArgumentException("Cannot replace an enum with itself");
        }
        this.alternateValueOfOwner = type;
        return this;
    }

    public EnumRenameBuilder rename(final String legacyName, final String newName) {
        this.enumFieldRenames.put(legacyName, newName);
        return this;
    }

    io.papermc.asm.rules.rename.EnumRenamer build() {
        return new EnumRenamer(this.enumTypeDesc, this.alternateValueOfOwner, Map.copyOf(this.enumFieldRenames));
    }
}
