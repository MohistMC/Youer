package ca.spottedleaf.dataconverter.types;

import ca.spottedleaf.dataconverter.types.json.JsonTypeUtil;
import ca.spottedleaf.dataconverter.types.nbt.NBTTypeUtil;

public interface Types {

    public static final NBTTypeUtil NBT = new NBTTypeUtil();

    public static final JsonTypeUtil JSON = new JsonTypeUtil(false);

    // why does this exist
    public static final JsonTypeUtil JSON_COMPRESSED = new JsonTypeUtil(true);
}
