package com.mohistmc.youer.api.gui;

import lombok.Getter;

/**
 * @author LSeng
 */
@Getter
public enum GUIType {

    ONEBYNINE(1),
    TWOBYNINE(2),
    THREEBYNINE(3),
    FOURBYNINE(4),
    FIVEBYNINE(5),
    SIXBYNINE(6),
    UNKNOWN(-1),
    CANCEL(0);

    private final int rows;

    GUIType(int rows) {
        this.rows = rows;
    }

    /**
     * Obtain the corresponding GUIType based on the number of rows
     *
     * @param rows count
     * @return The corresponding GUIType, if it cannot be found, returns UNKNOWN
     */
    public static GUIType fromRows(int rows) {
        for (GUIType type : values()) {
            if (type.getRows() == rows) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
