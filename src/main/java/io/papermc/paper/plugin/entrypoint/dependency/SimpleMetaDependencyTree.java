package io.papermc.paper.plugin.entrypoint.dependency;

import com.google.common.graph.MutableGraph;
import io.papermc.paper.plugin.configuration.PluginMeta;

public class SimpleMetaDependencyTree extends MetaDependencyTree {

    public SimpleMetaDependencyTree() {
    }

    public SimpleMetaDependencyTree(final MutableGraph<String> graph) {
        super(graph);
    }

    @Override
    protected void registerDependencies(final String identifier, final PluginMeta meta) {
        for (String dependency : meta.getPluginDependencies()) {
            this.graph.putEdge(identifier, dependency);
        }
        for (String dependency : meta.getPluginSoftDependencies()) {
            this.graph.putEdge(identifier, dependency);
        }
    }

    @Override
    protected void unregisterDependencies(final String identifier, final PluginMeta meta) {
        for (String dependency : meta.getPluginDependencies()) {
            this.graph.removeEdge(identifier, dependency);
        }
        for (String dependency : meta.getPluginSoftDependencies()) {
            this.graph.removeEdge(identifier, dependency);
        }
    }
}
