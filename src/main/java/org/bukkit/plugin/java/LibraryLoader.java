// CHECKSTYLE:OFF
package org.bukkit.plugin.java;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class LibraryLoader
{

    public LibraryLoader(@NotNull Logger logger)
    {

    }

    @Nullable
    public ClassLoader createLoader(@NotNull PluginDescriptionFile desc)
    {
        if ( desc.getLibraries().isEmpty() )
        {
            return null;
        }

        List<URL> jarFiles = new ArrayList<>();

        URLClassLoader loader = new URLClassLoader( jarFiles.toArray( new URL[ jarFiles.size() ] ), getClass().getClassLoader() );

        return loader;
    }
}
