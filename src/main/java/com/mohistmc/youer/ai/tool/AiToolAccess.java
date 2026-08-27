package com.mohistmc.youer.ai.tool;

import java.util.Objects;
import java.util.function.Predicate;

public final class AiToolAccess {

    private final Predicate<String> permissionCheck;

    public AiToolAccess(Predicate<String> permissionCheck) {
        this.permissionCheck = Objects.requireNonNull(permissionCheck, "permissionCheck");
    }

    public boolean permits(String permission) {
        return permissionCheck.test(permission);
    }
}
