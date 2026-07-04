package io.papermc.paper.log;

public final class LogManagerShutdownThread extends Thread {

    static LogManagerShutdownThread INSTANCE = new LogManagerShutdownThread();

    public static void hook() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Cannot re-hook after being unhooked");
        }
        // Eagerly resolve LoggerShutdown at hook() time, while the module system is
        // still open. If resolution were deferred to run() time and the NeoForge module
        // had already been closed (e.g. because server startup failed and the MinecraftServer
        // constructor — which calls unhook() — never executed), ModuleClassLoader would
        // throw NoClassDefFoundError. Referencing the class here keeps it in the JVM's
        // loaded-class cache so it survives module lifecycle changes.
        Class<?> ensureLoaded = LoggerShutdown.class;
        Runtime.getRuntime().addShutdownHook(INSTANCE);
    }

    public static void unhook() {
        Runtime.getRuntime().removeShutdownHook(INSTANCE);
        INSTANCE = null;
    }

    private LogManagerShutdownThread() {
        super("Log4j2 Shutdown Thread");
    }

    @Override
    public void run() {
        LoggerShutdown.shutdownLogging();
    }
}
