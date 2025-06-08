package ca.spottedleaf.dataconverter.minecraft.converters.itemstack;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import java.util.function.Function;

public final class ConverterEnchantmentsRename extends DataConverter<MapType<String>, MapType<String>> {

    private final Function<String, String> renamer;

    public ConverterEnchantmentsRename(final int toVersion, final Function<String, String> renamer) {
        this(toVersion, 0, renamer);
    }

    public ConverterEnchantmentsRename(final int toVersion, final int versionStep, final Function<String, String> renamer) {
        super(toVersion, versionStep);

        this.renamer = (final String input) -> {
            return renamer.apply(NamespaceUtil.correctNamespace(input));
        };
    }

    @Override
    public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
        final MapType<String> tag = data.getMap("tag");

        if (tag == null) {
            return null;
        }

        RenameHelper.renameListMapItems(tag, "Enchantments", "id", this.renamer);
        RenameHelper.renameListMapItems(tag, "StoredEnchantments", "id", this.renamer);

        return null;
    }
}
