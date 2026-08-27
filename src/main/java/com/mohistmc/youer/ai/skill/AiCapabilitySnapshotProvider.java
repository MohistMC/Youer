package com.mohistmc.youer.ai.skill;

import com.mohistmc.youer.ai.tool.AiExecutionDispatcher;
import com.mohistmc.youer.ai.tool.AiToolRegistry;
import com.mohistmc.youer.api.ai.tool.AiToolContext;
import com.mohistmc.youer.api.ai.tool.AiToolExecutionMode;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public final class AiCapabilitySnapshotProvider {

    private final AiExecutionDispatcher dispatcher;
    private final AiToolRegistry toolRegistry;
    private final AiSkillRegistry skillRegistry;
    private final Function<AiToolContext, AiSkillAccess> accessFactory;
    private final AiSkillIndex index;

    public AiCapabilitySnapshotProvider(
            AiExecutionDispatcher dispatcher,
            AiToolRegistry toolRegistry,
            AiSkillRegistry skillRegistry,
            Function<AiToolContext, AiSkillAccess> accessFactory,
            AiSkillIndex index) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.accessFactory = Objects.requireNonNull(accessFactory, "accessFactory");
        this.index = Objects.requireNonNull(index, "index");
    }

    public CompletionStage<AiCapabilitySnapshot> snapshot(AiToolContext context) {
        return snapshot(context, true);
    }

    public CompletionStage<AiCapabilitySnapshot> snapshot(AiToolContext context, boolean toolsEnabled) {
        return dispatcher.dispatch(AiToolExecutionMode.MAIN_THREAD,
                () -> CompletableFuture.completedFuture(capture(context, toolsEnabled)));
    }

    private AiCapabilitySnapshot capture(AiToolContext context, boolean toolsEnabled) {
        AiSkillAccess access = accessFactory.apply(context);
        AiToolRegistry.Snapshot tools = toolRegistry.snapshot(permission -> toolsEnabled
                        && access.hasPermission("youer.ai.use")
                        && access.hasPermission("youer.ai.tools.use")
                        && access.hasPermission(permission));
        AiSkillSnapshot skills = skillRegistry.snapshot(access, tools);
        return new AiCapabilitySnapshot(tools, skills, index.format(skills));
    }
}
