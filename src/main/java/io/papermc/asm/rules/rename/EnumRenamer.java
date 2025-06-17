package io.papermc.asm.rules.rename;

import java.lang.constant.ClassDesc;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public record EnumRenamer(ClassDesc typeDesc, @Nullable ClassDesc alternateValueOfOwner, Map<String, String> fieldRenames) {

    public EnumRenamer {
        fieldRenames = Map.copyOf(fieldRenames);
    }

    EnumRenamer overwrite(final EnumRenamer other) {
        if (!this.typeDesc.equals(other.typeDesc)) {
            throw new IllegalArgumentException("Cannot merge EnumRenamers with different typeDesc");
        } else if (!Objects.equals(this.alternateValueOfOwner, other.alternateValueOfOwner)) {
            throw new IllegalArgumentException("Cannot merge EnumRenamers with different alternateValueOfOwner");
        }
        final Map<String, String> newFieldRenames = new HashMap<>();
        newFieldRenames.putAll(this.fieldRenames);
        newFieldRenames.putAll(other.fieldRenames);
        return new EnumRenamer(this.typeDesc, this.alternateValueOfOwner, newFieldRenames);
    }
}
