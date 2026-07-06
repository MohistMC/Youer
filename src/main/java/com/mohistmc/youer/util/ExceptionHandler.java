package com.mohistmc.youer.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.BindException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static ExceptionHandler INSTANCE;
    private final Map<Class<? extends Throwable>, Consumer<Throwable>> exceptionHandlers = new HashMap<>();

    // ========== 线程堆栈采样器 (每100ms全量采样，环形缓冲区) ==========
    private static final int SAMPLE_SIZE = 30;
    private static final long SAMPLE_INTERVAL_MS = 100;
    private static final ThreadInfo[][] SAMPLE_RING = new ThreadInfo[SAMPLE_SIZE][];
    private static final AtomicInteger sampleIndex = new AtomicInteger(0);
    private static volatile boolean samplerStarted = false;

     private static void ensureSamplerStarted() {
        if (samplerStarted) return;
        synchronized (ExceptionHandler.class) {
            if (samplerStarted) return;
            samplerStarted = true;
            SAMPLE_RING[sampleIndex.getAndIncrement() % SAMPLE_SIZE] = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
            Thread sampler = new Thread(() -> {
                ThreadMXBean mx = ManagementFactory.getThreadMXBean();
                try {
                    for (int i = 0; i < SAMPLE_SIZE; i++) {
                        Thread.sleep(SAMPLE_INTERVAL_MS);
                        int idx = sampleIndex.getAndIncrement() % SAMPLE_SIZE;
                        SAMPLE_RING[idx] = mx.dumpAllThreads(false, false);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignored) {
                }
                samplerStarted = false;
            }, "EH-Sampler");
            sampler.setDaemon(true);
            sampler.setPriority(Thread.MIN_PRIORITY);
            sampler.start();
        }
    }

    public ExceptionHandler() {
        exceptionHandlers.put(OutOfMemoryError.class, this::handleOutOfMemoryError);
        exceptionHandlers.put(ClassNotFoundException.class, this::handleClassNotFoundException);
        exceptionHandlers.put(NoClassDefFoundError.class, this::handleNoClassDefFoundError);
        exceptionHandlers.put(BindException.class, this::handleBindException);
        exceptionHandlers.put(NullPointerException.class, this::handleNullPointerException);
        exceptionHandlers.put(SQLException.class, this::handleSQLException);
        exceptionHandlers.put(ConcurrentModificationException.class, this::handleConcurrentModificationException);
        // 包装现存线程的自定义 handler，使我们的 CME 检测也能触发
        Thread.setDefaultUncaughtExceptionHandler(this);
        INSTANCE = this;
        wrapExistingThreadHandlers();
    }

    /**
     * 遍历所有现存线程，将它们的自定义 UncaughtExceptionHandler 包一层，
     * 使 CME 异常也能被我们的处理器捕获
     */
    private static void wrapExistingThreadHandlers() {
        Thread.UncaughtExceptionHandler ourHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (ourHandler == null) return;
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for (Thread t : threads) {
            Thread.UncaughtExceptionHandler existing = t.getUncaughtExceptionHandler();
            // 只包装那些设置了自定义 handler 且不是默认 handler 的线程
            if (existing != null && existing != ourHandler && !(existing instanceof ExceptionHandler)) {
                t.setUncaughtExceptionHandler((thread, throwable) -> {
                    if (throwable instanceof ConcurrentModificationException) {
                        ourHandler.uncaughtException(thread, throwable);
                    }
                    existing.uncaughtException(thread, throwable);
                });
            }
        }
    }

     @Override
    public void uncaughtException(Thread t, Throwable e) {
        // 先检查异常本身或其 Cause 链中是否有 CME
        Throwable cme = findCauseOfType(e, ConcurrentModificationException.class);
        Consumer<Throwable> handler = null;
        if (cme != null) {
            // 有 CME 则用 CME 处理器，同时打印完整的异常信息
            LOGGER.error("========== 检测到异常 (Caused by: ConcurrentModificationException) ==========");
            LOGGER.error("外层异常: " + e.getClass().getName() + ": " + e.getMessage());
            handleConcurrentModificationException(cme);
        }
        // 仍然尝试匹配精确的处理器
        handler = findHandler(e.getClass());
        if (handler != null) {
            if (cme == null) handler.accept(e);
        } else if (cme == null) {
            LOGGER.error("========== 未处理的异常 ==========");
            LOGGER.error("异常类型: " + e.getClass().getName());
            LOGGER.error("异常信息: " + e.getMessage());
            LOGGER.error("发生线程: " + t.getName() + " (ID: " + t.getId() + ")");
            LOGGER.error("-------- 完整异常链 (Caused by) --------");
            printCauseChain(e);
            LOGGER.error("-------- 完整堆栈 --------");
            e.printStackTrace();
            LOGGER.error("-------- 相关代码位置 (按调用顺序) --------");
            printAllRelevantFrames(e);
            LOGGER.error("========================================");
        }
    }

   /**
     * 在异常链中查找指定类型的异常（递归遍历 Cause）
     */
    private static <T extends Throwable> T findCauseOfType(Throwable e, Class<T> type) {
        if (e == null) return null;
        if (type.isInstance(e)) return type.cast(e);
        return findCauseOfType(e.getCause(), type);
    }

    /**
     * 静态便捷方法：检查异常链中是否有 CME，有则触发分析。
     * 可在 try-catch 或框架的异常处理器中手动调用。
     */
     public static void onException(Throwable e) {
        if (INSTANCE == null) return;
        Throwable cme = findCauseOfType(e, ConcurrentModificationException.class);
        if (cme != null) {
            ensureSamplerStarted();
            ThreadInfo[] snapshot = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
            LOGGER.error("========== [手动调用] 检测到 CME ==========");
            LOGGER.error("外层异常: " + e.getClass().getName() + ": " + e.getMessage());
            INSTANCE.printCMEAnalysis(cme, snapshot);
        }
    }

    private void printCMEAnalysis(Throwable cme, ThreadInfo[] snapshot) {
        LOGGER.error("异常信息: " + cme.getMessage());
        LOGGER.error("发生线程: " + Thread.currentThread().getName() + " (ID: " + Thread.currentThread().getId() + ")");
        LOGGER.error("-------- 完整异常链 (Caused by) --------");
        printCauseChain(cme);
        LOGGER.error("-------- 完整堆栈 --------");
        cme.printStackTrace(System.out);
        LOGGER.error("-------- 相关代码位置 (所有层级) --------");
        printAllRelevantFrames(cme);
        LOGGER.error("-------- 历史线程采样 (最近3秒，每100ms一次) --------");
        dumpRecentThreadSamples();
        LOGGER.error("-------- 当前线程快照 --------");
        printThreadSnapshot(snapshot);
        LOGGER.error("\n-------- 修复建议 --------");
        LOGGER.error("1. 如果是在 for-each 循环中删除了元素，请改为使用 Iterator.remove() 或 使用 CopyOnWriteArrayList / ConcurrentHashMap。");
        LOGGER.error("2. 如果是多线程环境，请检查其他线程堆栈中是否有同时操作相同集合的代码。");
        LOGGER.error("================================================");
    }

    /**
     * 递归查找匹配的异常处理器，支持父类/接口匹配
     */
    private Consumer<Throwable> findHandler(Class<?> clazz) {
        if (clazz == null || clazz == Throwable.class) return null;
        Consumer<Throwable> handler = exceptionHandlers.get(clazz);
        if (handler != null) return handler;
        // 尝试父类
        handler = findHandler(clazz.getSuperclass());
        if (handler != null) return handler;
        // 尝试接口
        for (Class<?> iface : clazz.getInterfaces()) {
            handler = findHandler(iface);
            if (handler != null) return handler;
        }
        return null;
    }

    /**
     * 打印完整的 Caused by 异常链
     */
    private void printCauseChain(Throwable e) {
        Throwable cause = e;
        int level = 0;
        while (cause != null) {
            if (level > 0) {
                LOGGER.error("  ↓ Caused by (第" + level + "层):");
            }
            LOGGER.error("    " + cause.getClass().getName() + ": " + cause.getMessage());
            printRelevantFrames(cause, "      ");
            cause = cause.getCause();
            level++;
        }
    }

    /**
     * 打印异常堆栈中所有非内部框架的类路径定位
     */
    private void printJarOrClassInfo(Throwable e) {
        printRelevantFrames(e, "");
    }

    private void printRelevantFrames(Throwable e, String indent) {
        Set<String> printed = new HashSet<>();
        for (StackTraceElement element : e.getStackTrace()) {
            if (!isInternalJavaClass(element.getClassName()) && printed.add(element.getClassName())) {
                LOGGER.error(indent + "类: " + element.getClassName() + " (" + element.getFileName() + ":" + element.getLineNumber() + ")");
                printJarLocation(element.getClassName(), indent);
            }
        }
    }

    /**
     * 打印异常链中所有相关代码位置（每层都扫，不限于最上层）
     */
    private void printAllRelevantFrames(Throwable e) {
        Set<String> printedJars = new HashSet<>();
        Throwable cursor = e;
        int level = 0;
        while (cursor != null) {
            if (level > 0) {
                LOGGER.error("  --- Caused by: " + cursor.getClass().getSimpleName() + " ---");
            }
            for (StackTraceElement element : cursor.getStackTrace()) {
                if (!isInternalJavaClass(element.getClassName()) && printedJars.add(element.getClassName())) {
                    LOGGER.error("  -> " + element.getClassName() + "." + element.getMethodName()
                            + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
                    printJarLocation(element.getClassName(), "     来源: ");
                }
            }
            cursor = cursor.getCause();
            level++;
        }
    }

    private void printJarLocation(String className, String indent) {
        try {
            Class<?> clazz = Class.forName(className);
            String location = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
            // 简化路径：只保留 jar 文件名或关键路径
            if (location.endsWith(".jar")) {
                String jarName = location.substring(location.lastIndexOf('/') + 1);
                LOGGER.error(indent + "Jar: " + jarName);
            } else {
                LOGGER.error(indent + "路径: " + location);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isInternalJavaClass(String className) {
        return className.startsWith("java.") || className.startsWith("javax.") ||
                className.startsWith("org.w3c.dom.") || className.startsWith("org.xml.") ||
                className.startsWith("com.sun.") || className.startsWith("sun.") ||
                className.startsWith("javafx.") ||
                className.startsWith("jdk.internal.") || className.startsWith("jdk.jfr.");
    }
    
    private void handleOutOfMemoryError(Throwable e) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        LOGGER.error("你给Minecraft服务器分配的内存不够。");
        LOGGER.error("当前已用内存: " + usedMemory + " MB");
        LOGGER.error("当前总分配内存: " + totalMemory + " MB");
        LOGGER.error("JVM最大可用内存: " + maxMemory + " MB");
        printJarOrClassInfo(e);
    }

    private void handleClassNotFoundException(Throwable e) {
        LOGGER.error("未找到对应的Java类: " + e.getMessage());
        LOGGER.error("请尝试查找对应的类是否是客户端独有，例如包含\"GUI\"等字样，如果确信是Mod或插件问题，请联系对应开发者。");
        printJarOrClassInfo(e);
    }

    private void handleNoClassDefFoundError(Throwable e) {
        LOGGER.error("运行时未找到类: " + e.getMessage());
        LOGGER.error("这可能是因为类在编译时存在，但在运行时由于依赖问题、类路径不完整等原因导致找不到。");
        printJarOrClassInfo(e);
    }

    private void handleBindException(Throwable e) {
        LOGGER.error("无法将服务器绑定到: " + e.getMessage());
        LOGGER.error("这可能是因为端口已经被占用，或者没有足够的权限绑定到指定的端口。");
        LOGGER.error("请检查是否有其他程序占用了相同的端口，或者尝试使用不同的端口启动服务器。");
    }

    private void handleNullPointerException(Throwable e) {
        LOGGER.error("你遇到了一个NullPointerException，这个错误可能与未正确初始化的对象有关。");
        LOGGER.error("以下是完整的异常信息:");
        e.printStackTrace();
        printJarOrClassInfo(e);
    }

    private void handleSQLException(Throwable e) {
        SQLException sqlException = (SQLException) e;
        LOGGER.error("看起来与数据库连接时出现了问题。");
        LOGGER.error("SQL状态码：{}", sqlException.getErrorCode());
        LOGGER.error("SQL返回：" + sqlException.getMessage());
        sqlException.printStackTrace();
        printJarOrClassInfo(e);
    }

    /**
     * 增强版 ConcurrentModificationException 处理
     * 打印当前所有线程堆栈，帮助定位并发修改的源头
     */
      private void handleConcurrentModificationException(Throwable e) {
        ensureSamplerStarted();
        ThreadInfo[] snapshot = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
        printCMEAnalysis(e, snapshot);
    }

    private void dumpRecentThreadSamples() {
        int total = Math.min(sampleIndex.get(), SAMPLE_SIZE);
        if (total == 0) {
            LOGGER.error("  (采样器刚启动，暂无历史数据，稍后重试即可)");
            return;
        }
        LOGGER.error("  共 {} 次采样 (每次间隔 {}ms，覆盖最近约 {}ms)", total, SAMPLE_INTERVAL_MS, total * SAMPLE_INTERVAL_MS);
        LOGGER.error("  按时间倒序展示，优先展示包含集合写入操作的线程:");

        // 合并所有采样中出现的线程，按时间倒序输出
        Set<String> printedThreads = new LinkedHashSet<>();
        for (int i = 0; i < total; i++) {
            int idx = (sampleIndex.get() - 1 - i) % SAMPLE_SIZE;
            if (idx < 0) idx += SAMPLE_SIZE;
            ThreadInfo[] sample = SAMPLE_RING[idx];
            if (sample == null) continue;
            long sampleTime = System.currentTimeMillis() - i * SAMPLE_INTERVAL_MS;

            for (ThreadInfo info : sample) {
                if (info.getStackTrace().length == 0) continue;
                String tid = info.getThreadName() + ":" + info.getThreadId();
                if (!printedThreads.add(tid)) continue;

                // 只展示有集合写入操作的线程（put/remove等），或非内部类的线程
                boolean hasWriteOp = false;
                boolean hasUserCode = false;
                for (StackTraceElement ste : info.getStackTrace()) {
                    String cn = ste.getClassName();
                    if (!isInternalJavaClass(cn)) hasUserCode = true;
                    if (cn.startsWith("java.util.") && (ste.getMethodName().equals("put") || ste.getMethodName().equals("putVal")
                            || ste.getMethodName().equals("remove") || ste.getMethodName().equals("add")
                            || ste.getMethodName().equals("clear") || ste.getMethodName().equals("resize"))) {
                        hasWriteOp = true;
                    }
                }
                if (!hasWriteOp && !hasUserCode) continue;

                String tag = hasWriteOp ? " <-- 含有集合写入操作" : "";
               LOGGER.error("\n  [~" + (i * SAMPLE_INTERVAL_MS) + "ms前] 线程: {} (状态: {}){}", info.getThreadName(), info.getThreadState(), tag);
                for (StackTraceElement ste : info.getStackTrace()) {
                    if (isInternalJavaClass(ste.getClassName())) continue;
                    LOGGER.error("    at {}.{}({}:{})", ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber());
                }
            }
        }
    }

    /**
     * 打印线程快照，标记集合写入操作
     */
    private void printThreadSnapshot(ThreadInfo[] threadInfos) {
        for (ThreadInfo info : threadInfos) {
            if (info.getStackTrace().length == 0) continue;

            LOGGER.error("\n--- 线程: {} (ID: {}, 状态: {}) ---", info.getThreadName(), info.getThreadId(), info.getThreadState());
            int count = 0;
            boolean hasNonInternal = false;
            for (StackTraceElement ste : info.getStackTrace()) {
                if (!isInternalJavaClass(ste.getClassName())) {
                    hasNonInternal = true;
                    break;
                }
            }

            for (StackTraceElement ste : info.getStackTrace()) {
                // 优先展示非内部类帧；若线程全是内部类帧，则展示所有帧
                if (hasNonInternal && isInternalJavaClass(ste.getClassName())) continue;

                String cn = ste.getClassName();
                String methodSuffix = "";
                if (cn.startsWith("java.util.") && (ste.getMethodName().equals("put") || ste.getMethodName().equals("putVal")
                        || ste.getMethodName().equals("remove") || ste.getMethodName().equals("add")
                        || ste.getMethodName().equals("clear") || ste.getMethodName().equals("resize"))) {
                    methodSuffix = " <-- 集合写入操作";
                }

               LOGGER.error("  -> {}.{}({}:{}){}", cn, ste.getMethodName(), ste.getFileName(), ste.getLineNumber(), methodSuffix);
            }
        }
    }

}
