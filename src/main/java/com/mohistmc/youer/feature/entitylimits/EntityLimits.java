package com.mohistmc.youer.feature.entitylimits;

import lombok.Data;
/**
 * @author Mgazul
 * @date 2025/10/2 21:22
 */
@Data
public class EntityLimits {

    public String worldName;
    public String entityName;
    public int entityLimit;

    public EntityLimits(String worldName, String entityName, int entityLimit) {
        this.worldName = worldName;
        this.entityName = entityName;
        this.entityLimit = entityLimit;
    }
}
