package ca.spottedleaf.dataconverter.minecraft.hooks;

import ca.spottedleaf.converter.datatypes.DataHook;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;

public class DataHookValueTypeEnforceNamespaced extends DataHook<Object, Object> {

    @Override
    public Object preHook(final Object data, final long fromVersion, final long toVersion) {
        if (data instanceof String string) {
            return NamespaceUtil.correctNamespaceOrNull(string);
        }
        return null;
    }

    @Override
    public Object postHook(final Object data, final long fromVersion, final long toVersion) {
        return null;
    }
}
