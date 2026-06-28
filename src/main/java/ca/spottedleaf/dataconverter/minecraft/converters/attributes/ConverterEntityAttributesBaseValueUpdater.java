package ca.spottedleaf.dataconverter.minecraft.converters.attributes;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import java.util.function.DoubleUnaryOperator;

public final class ConverterEntityAttributesBaseValueUpdater extends DataConverter<MapType, MapType> {

    private final String targetId;
    private final DoubleUnaryOperator updater;

    public ConverterEntityAttributesBaseValueUpdater(final int toVersion, final String targetId, final DoubleUnaryOperator updater) {
        this(toVersion, 0, targetId, updater);
    }

    public ConverterEntityAttributesBaseValueUpdater(final int toVersion, final int versionStep, final String targetId,
                                                     final DoubleUnaryOperator updater) {
        super(toVersion, versionStep);
        this.targetId = targetId;
        this.updater = updater;
    }

    @Override
    public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
        final ListType modifiers = data.getList("attributes", ObjectType.MAP);
        if (modifiers == null) {
            return null;
        }

        for (int i = 0, len = modifiers.size(); i < len; ++i) {
            final MapType modifier = modifiers.getMap(i);

            if (!this.targetId.equals(NamespaceUtil.correctNamespace(modifier.getString("id", "")))) {
                continue;
            }

            modifier.setDouble("base", this.updater.applyAsDouble(modifier.getDouble("base", 0.0)));
        }

        return null;
    }
}
