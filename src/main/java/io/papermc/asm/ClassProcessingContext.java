package io.papermc.asm;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ClassProcessingContext {
    ClassInfoProvider classInfoProvider();

    /**
     * Returns the name of the class currently being processed. Only
     * available once the class header has been visited.
     *
     * @return name of the class currently being processed
     */
    String processingClassName();

    /**
     * Returns the name of the super class of the class currently being processed. Only
     * available once the class header has been visited.
     *
     * @return name of the super class of the class currently being processed
     */
    @Nullable String processingClassSuperClassName();
}
