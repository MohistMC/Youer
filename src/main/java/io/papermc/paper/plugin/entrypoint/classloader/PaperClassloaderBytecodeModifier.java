package io.papermc.paper.plugin.entrypoint.classloader;

// Stub, implement in future.
public class PaperClassloaderBytecodeModifier implements ClassloaderBytecodeModifier {

    @Override
    public byte[] modify(byte[] bytecode) {
        return io.papermc.paper.pluginremap.reflect.ReflectionRemapper.processClass(bytecode);
    }
}
