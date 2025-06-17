package io.papermc.paper.pluginremap;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.neoforged.srgutils.IMappingFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
final class ReobfServer {

    private final CompletableFuture<Void> load;

    ReobfServer(final CompletableFuture<IMappingFile> mappings, final Executor executor) {
        this.load = CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Path> remapped() {
        return this.load.thenApply($ -> this.remappedPath());
    }

    private Path remappedPath() {
        return new File("libraries/com/mohistmc/paper/ReobfServer.jar").toPath();
    }
}
