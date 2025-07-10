package ca.spottedleaf.concurrentutil.completable;

import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.concurrentutil.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Completable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Completable.class);
    private static final Function<? super Throwable, ? extends Throwable> DEFAULT_EXCEPTION_HANDLER = (final Throwable thr) -> {
        LOGGER.error("Unhandled exception during Completable operation", thr);
        return thr;
    };

    public static Executor getDefaultExecutor() {
        return ForkJoinPool.commonPool();
    }

    private static final Transform<?, ?> COMPLETED_STACK = new Transform<>(null, null, null, null) {
        @Override
        public void run() {}
    };
    private volatile Transform<?, T> completeStack;
    private static final VarHandle COMPLETE_STACK_HANDLE = ConcurrentUtil.getVarHandle(Completable.class, "completeStack", Transform.class);

    private static final Object NULL_MASK = new Object();
    private volatile Object result;
    private static final VarHandle RESULT_HANDLE = ConcurrentUtil.getVarHandle(Completable.class, "result", Object.class);

    private Object getResultPlain() {
        return (Object)RESULT_HANDLE.get(this);
    }

    private Object getResultVolatile() {
        return (Object)RESULT_HANDLE.getVolatile(this);
    }

    private void pushStackOrRun(final Transform<?, T> push) {
        int failures = 0;
        for (Transform<?, T> curr = (Transform<?, T>)COMPLETE_STACK_HANDLE.getVolatile(this);;) {
            if (curr == COMPLETED_STACK) {
                push.execute();
                return;
            }

            push.next = curr;

            for (int i = 0; i < failures; ++i) {
                ConcurrentUtil.backoff();
            }

            if (curr == (curr = (Transform<?, T>)COMPLETE_STACK_HANDLE.compareAndExchange(this, curr, push))) {
                return;
            }
            push.next = null;
            ++failures;
        }
    }

    private void propagateStack() {
        Transform<?, T> topStack = (Transform<?, T>)COMPLETE_STACK_HANDLE.getAndSet(this, COMPLETED_STACK);
        while (topStack != null) {
            topStack.execute();
            topStack = topStack.next;
        }
    }

    private static Object maskNull(final Object res) {
        return res == null ? NULL_MASK : res;
    }

    private static Object unmaskNull(final Object res) {
        return res == NULL_MASK ? null : res;
    }

    private static Executor checkExecutor(final Executor executor) {
        return Validate.notNull(executor, "Executor may not be null");
    }

    public Completable() {}

    private Completable(final Object complete) {
        COMPLETE_STACK_HANDLE.set(this, COMPLETED_STACK);
        RESULT_HANDLE.setRelease(this, complete);
    }

    public static <T> Completable<T> completed(final T value) {
        return new Completable<>(maskNull(value));
    }

    public static <T> Completable<T> failed(final Throwable ex) {
        Validate.notNull(ex, "Exception may not be null");

        return new Completable<>(new ExceptionResult(ex));
    }

    public static <T> Completable<T> supplied(final Supplier<T> supplier) {
        return supplied(supplier, DEFAULT_EXCEPTION_HANDLER);
    }

    public static <T> Completable<T> supplied(final Supplier<T> supplier, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        try {
            return completed(supplier.get());
        } catch (final Throwable throwable) {
            Throwable complete;
            try {
                complete = exceptionHandler.apply(throwable);
            } catch (final Throwable thr2) {
                throwable.addSuppressed(thr2);
                complete = throwable;
            }
            return failed(complete);
        }
    }

    public static <T> Completable<T> suppliedAsync(final Supplier<T> supplier, final Executor executor) {
        return suppliedAsync(supplier, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public static <T> Completable<T> suppliedAsync(final Supplier<T> supplier, final Executor executor, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        final Completable<T> ret = new Completable<>();

        class AsyncSuppliedCompletable implements Runnable, CompletableFuture.AsynchronousCompletionTask {
            @Override
            public void run() {
                try {
                    ret.complete(supplier.get());
                } catch (final Throwable throwable) {
                    Throwable complete;
                    try {
                        complete = exceptionHandler.apply(throwable);
                    } catch (final Throwable thr2) {
                        throwable.addSuppressed(thr2);
                        complete = throwable;
                    }
                    ret.completeExceptionally(complete);
                }
            }
        }

        try {
            executor.execute(new AsyncSuppliedCompletable());
        } catch (final Throwable throwable) {
            Throwable complete;
            try {
                complete = exceptionHandler.apply(throwable);
            } catch (final Throwable thr2) {
                throwable.addSuppressed(thr2);
                complete = throwable;
            }
            ret.completeExceptionally(complete);
        }

        return ret;
    }

    private boolean completeRaw(final Object value) {
        if ((Object)RESULT_HANDLE.getVolatile(this) != null || !(boolean)RESULT_HANDLE.compareAndSet(this, (Object)null, value)) {
            return false;
        }

        this.propagateStack();
        return true;
    }

    public boolean complete(final T result) {
        return this.completeRaw(maskNull(result));
    }

    public boolean completeExceptionally(final Throwable exception) {
        Validate.notNull(exception, "Exception may not be null");

        return this.completeRaw(new ExceptionResult(exception));
    }

    public boolean isDone() {
        return this.getResultVolatile() != null;
    }

    public boolean isNormallyComplete() {
        return this.getResultVolatile() != null && !(this.getResultVolatile() instanceof ExceptionResult);
    }

    public boolean isExceptionallyComplete() {
        return this.getResultVolatile() instanceof ExceptionResult;
    }

    public Throwable getException() {
        final Object res = this.getResultVolatile();
        if (res == null) {
            return null;
        }

        if (!(res instanceof ExceptionResult exRes)) {
            throw new IllegalStateException("Not completed exceptionally");
        }

        return exRes.ex;
    }

    public T getNow(final T dfl) throws CompletionException {
        final Object res = this.getResultVolatile();
        if (res == null) {
            return dfl;
        }

        if (res instanceof ExceptionResult exRes) {
            throw new CompletionException(exRes.ex);
        }

        return (T)unmaskNull(res);
    }

    public T join() throws CompletionException {
        if (this.isDone()) {
            return this.getNow(null);
        }

        final UnparkTransform<T> unparkTransform = new UnparkTransform<>(this, Thread.currentThread());

        this.pushStackOrRun(unparkTransform);

        boolean interuptted = false;
        while (!unparkTransform.isReleasable()) {
            try {
                ForkJoinPool.managedBlock(unparkTransform);
            } catch (final InterruptedException ex) {
                interuptted = true;
            }
        }

        if (interuptted) {
            Thread.currentThread().interrupt();
        }

        return this.getNow(null);
    }

    public CompletableFuture<T> toFuture() {
        final Object rawResult = this.getResultVolatile();
        if (rawResult != null) {
            if (rawResult instanceof ExceptionResult exRes) {
                return CompletableFuture.failedFuture(exRes.ex);
            } else {
                return CompletableFuture.completedFuture((T)unmaskNull(rawResult));
            }
        }

        final CompletableFuture<T> ret = new CompletableFuture<>();

        class ToFuture implements BiConsumer<T, Throwable> {

            @Override
            public void accept(final T res, final Throwable ex) {
                if (ex != null) {
                    ret.completeExceptionally(ex);
                } else {
                    ret.complete(res);
                }
            }
        }

        this.whenComplete(new ToFuture());

        return ret;
    }

    public static <T> Completable<T> fromFuture(final CompletionStage<T> stage) {
        final Completable<T> ret = new Completable<>();

        class FromFuture implements BiConsumer<T, Throwable> {
            @Override
            public void accept(final T res, final Throwable ex) {
                if (ex != null) {
                    ret.completeExceptionally(ex);
                } else {
                    ret.complete(res);
                }
            }
        }

        stage.whenComplete(new FromFuture());

        return ret;
    }


    public <U> Completable<U> thenApply(final Function<? super T, ? extends U> function) {
        return this.thenApply(function, DEFAULT_EXCEPTION_HANDLER);
    }

    public <U> Completable<U> thenApply(final Function<? super T, ? extends U> function, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(function, "Function may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<U> ret = new Completable<>();
        this.pushStackOrRun(new ApplyTransform<>(null, this, ret, exceptionHandler, function));
        return ret;
    }

    public <U> Completable<U> thenApplyAsync(final Function<? super T, ? extends U> function) {
        return this.thenApplyAsync(function, getDefaultExecutor(), DEFAULT_EXCEPTION_HANDLER);
    }

    public <U> Completable<U> thenApplyAsync(final Function<? super T, ? extends U> function, final Executor executor) {
        return this.thenApplyAsync(function, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public <U> Completable<U> thenApplyAsync(final Function<? super T, ? extends U> function, final Executor executor, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(function, "Function may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<U> ret = new Completable<>();
        this.pushStackOrRun(new ApplyTransform<>(checkExecutor(executor), this, ret, exceptionHandler, function));
        return ret;
    }


    public Completable<Void> thenAccept(final Consumer<? super T> consumer) {
        return this.thenAccept(consumer, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<Void> thenAccept(final Consumer<? super T> consumer, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(consumer, "Consumer may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<Void> ret = new Completable<>();
        this.pushStackOrRun(new AcceptTransform<>(null, this, ret, exceptionHandler, consumer));
        return ret;
    }

    public Completable<Void> thenAcceptAsync(final Consumer<? super T> consumer) {
        return this.thenAcceptAsync(consumer, getDefaultExecutor(), DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<Void> thenAcceptAsync(final Consumer<? super T> consumer, final Executor executor) {
        return this.thenAcceptAsync(consumer, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<Void> thenAcceptAsync(final Consumer<? super T> consumer, final Executor executor, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(consumer, "Consumer may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<Void> ret = new Completable<>();
        this.pushStackOrRun(new AcceptTransform<>(checkExecutor(executor), this, ret, exceptionHandler, consumer));
        return ret;
    }


    public Completable<Void> thenRun(final Runnable run) {
        return this.thenRun(run, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<Void> thenRun(final Runnable run, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(run, "Run may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<Void> ret = new Completable<>();
        this.pushStackOrRun(new RunTransform<>(null, this, ret, exceptionHandler, run));
        return ret;
    }

    public Completable<Void> thenRunAsync(final Runnable run) {
        return this.thenRunAsync(run, getDefaultExecutor(), DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<Void> thenRunAsync(final Runnable run, final Executor executor) {
        return this.thenRunAsync(run, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<Void> thenRunAsync(final Runnable run, final Executor executor, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(run, "Run may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<Void> ret = new Completable<>();
        this.pushStackOrRun(new RunTransform<>(checkExecutor(executor), this, ret, exceptionHandler, run));
        return ret;
    }


    public <U> Completable<U> handle(final BiFunction<? super T, ? super Throwable, ? extends U> function) {
        return this.handle(function, DEFAULT_EXCEPTION_HANDLER);
    }

    public <U> Completable<U> handle(final BiFunction<? super T, ? super Throwable, ? extends U> function,
                                     final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(function, "Function may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<U> ret = new Completable<>();
        this.pushStackOrRun(new HandleTransform<>(null, this, ret, exceptionHandler, function));
        return ret;
    }

    public <U> Completable<U> handleAsync(final BiFunction<? super T, ? super Throwable, ? extends U> function) {
        return this.handleAsync(function, getDefaultExecutor(), DEFAULT_EXCEPTION_HANDLER);
    }

    public <U> Completable<U> handleAsync(final BiFunction<? super T, ? super Throwable, ? extends U> function,
                                          final Executor executor) {
        return this.handleAsync(function, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public <U> Completable<U> handleAsync(final BiFunction<? super T, ? super Throwable, ? extends U> function,
                                          final Executor executor,
                                          final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(function, "Function may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<U> ret = new Completable<>();
        this.pushStackOrRun(new HandleTransform<>(checkExecutor(executor), this, ret, exceptionHandler, function));
        return ret;
    }


    public Completable<T> whenComplete(final BiConsumer<? super T, ? super Throwable> consumer) {
        return this.whenComplete(consumer, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<T> whenComplete(final BiConsumer<? super T, ? super Throwable> consumer, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(consumer, "Consumer may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<T> ret = new Completable<>();
        this.pushStackOrRun(new WhenTransform<>(null, this, ret, exceptionHandler, consumer));
        return ret;
    }

    public Completable<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> consumer) {
        return this.whenCompleteAsync(consumer, getDefaultExecutor(), DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> consumer, final Executor executor) {
        return this.whenCompleteAsync(consumer, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> consumer, final Executor executor,
                                            final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(consumer, "Consumer may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<T> ret = new Completable<>();
        this.pushStackOrRun(new WhenTransform<>(checkExecutor(executor), this, ret, exceptionHandler, consumer));
        return ret;
    }


    public Completable<T> exceptionally(final Function<Throwable, ? extends T> function) {
        return this.exceptionally(function, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<T> exceptionally(final Function<Throwable, ? extends T> function, final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(function, "Function may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<T> ret = new Completable<>();
        this.pushStackOrRun(new ExceptionallyTransform<>(null, this, ret, exceptionHandler, function));
        return ret;
    }

    public Completable<T> exceptionallyAsync(final Function<Throwable, ? extends T> function) {
        return this.exceptionallyAsync(function, getDefaultExecutor(), DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<T> exceptionallyAsync(final Function<Throwable, ? extends T> function, final Executor executor) {
        return this.exceptionallyAsync(function, executor, DEFAULT_EXCEPTION_HANDLER);
    }

    public Completable<T> exceptionallyAsync(final Function<Throwable, ? extends T> function, final Executor executor,
                                             final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
        Validate.notNull(function, "Function may not be null");
        Validate.notNull(exceptionHandler, "Exception handler may not be null");

        final Completable<T> ret = new Completable<>();
        this.pushStackOrRun(new ExceptionallyTransform<>(checkExecutor(executor), this, ret, exceptionHandler, function));
        return ret;
    }

    private static final class ExceptionResult {
        public final Throwable ex;

        public ExceptionResult(final Throwable ex) {
            this.ex = ex;
        }
    }

    private static abstract class Transform<U, T> implements Runnable, CompletableFuture.AsynchronousCompletionTask {

        private Transform<?, T> next;

        private final Executor executor;
        protected final Completable<T> from;
        protected final Completable<U> to;
        protected final Function<? super Throwable, ? extends Throwable> exceptionHandler;

        protected Transform(final Executor executor, final Completable<T> from, final Completable<U> to,
                            final Function<? super Throwable, ? extends Throwable> exceptionHandler) {
            this.executor = executor;
            this.from = from;
            this.to = to;
            this.exceptionHandler = exceptionHandler;
        }

        // force interface call to become virtual call
        @Override
        public abstract void run();

        protected void failed(final Throwable throwable) {
            Throwable complete;
            try {
                complete = this.exceptionHandler.apply(throwable);
            } catch (final Throwable thr2) {
                throwable.addSuppressed(thr2);
                complete = throwable;
            }
            this.to.completeExceptionally(complete);
        }

        public void execute() {
            if (this.executor == null) {
                this.run();
                return;
            }

            try {
                this.executor.execute(this);
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class ApplyTransform<U, T> extends Transform<U, T> {

        private final Function<? super T, ? extends U> function;

        public ApplyTransform(final Executor executor, final Completable<T> from, final Completable<U> to,
                              final Function<? super Throwable, ? extends Throwable> exceptionHandler,
                              final Function<? super T, ? extends U> function) {
            super(executor, from, to, exceptionHandler);
            this.function = function;
        }

        @Override
        public void run() {
            final Object result = this.from.getResultPlain();
            try {
                if (result instanceof ExceptionResult exRes) {
                    this.to.completeExceptionally(exRes.ex);
                } else {
                    this.to.complete(this.function.apply((T)unmaskNull(result)));
                }
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class AcceptTransform<T> extends Transform<Void, T> {
        private final Consumer<? super T> consumer;

        public AcceptTransform(final Executor executor, final Completable<T> from, final Completable<Void> to,
                               final Function<? super Throwable, ? extends Throwable> exceptionHandler,
                               final Consumer<? super T> consumer) {
            super(executor, from, to, exceptionHandler);
            this.consumer = consumer;
        }

        @Override
        public void run() {
            final Object result = this.from.getResultPlain();
            try {
                if (result instanceof ExceptionResult exRes) {
                    this.to.completeExceptionally(exRes.ex);
                } else {
                    this.consumer.accept((T)unmaskNull(result));
                    this.to.complete(null);
                }
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class RunTransform<T> extends Transform<Void, T> {
        private final Runnable run;

        public RunTransform(final Executor executor, final Completable<T> from, final Completable<Void> to,
                            final Function<? super Throwable, ? extends Throwable> exceptionHandler,
                            final Runnable run) {
            super(executor, from, to, exceptionHandler);
            this.run = run;
        }

        @Override
        public void run() {
            final Object result = this.from.getResultPlain();
            try {
                if (result instanceof ExceptionResult exRes) {
                    this.to.completeExceptionally(exRes.ex);
                } else {
                    this.run.run();
                    this.to.complete(null);
                }
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class HandleTransform<U, T> extends Transform<U, T> {

        private final BiFunction<? super T, ? super Throwable, ? extends U> function;

        public HandleTransform(final Executor executor, final Completable<T> from, final Completable<U> to,
                               final Function<? super Throwable, ? extends Throwable> exceptionHandler,
                               final BiFunction<? super T, ? super Throwable, ? extends U> function) {
            super(executor, from, to, exceptionHandler);
            this.function = function;
        }

        @Override
        public void run() {
            final Object result = this.from.getResultPlain();
            try {
                if (result instanceof ExceptionResult exRes) {
                    this.to.complete(this.function.apply(null, exRes.ex));
                } else {
                    this.to.complete(this.function.apply((T)unmaskNull(result), null));
                }
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class WhenTransform<T> extends Transform<T, T> {

        private final BiConsumer<? super T, ? super Throwable> consumer;

        public WhenTransform(final Executor executor, final Completable<T> from, final Completable<T> to,
                              final Function<? super Throwable, ? extends Throwable> exceptionHandler,
                              final BiConsumer<? super T, ? super Throwable> consumer) {
            super(executor, from, to, exceptionHandler);
            this.consumer = consumer;
        }

        @Override
        public void run() {
            final Object result = this.from.getResultPlain();
            try {
                if (result instanceof ExceptionResult exRes) {
                    this.consumer.accept(null, exRes.ex);
                    this.to.completeExceptionally(exRes.ex);
                } else {
                    final T unmasked = (T)unmaskNull(result);
                    this.consumer.accept(unmasked, null);
                    this.to.complete(unmasked);
                }
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class ExceptionallyTransform<T> extends Transform<T, T> {
        private final Function<Throwable, ? extends T> function;

        public ExceptionallyTransform(final Executor executor, final Completable<T> from, final Completable<T> to,
                              final Function<? super Throwable, ? extends Throwable> exceptionHandler,
                              final Function<Throwable, ? extends T> function) {
            super(executor, from, to, exceptionHandler);
            this.function = function;
        }

        @Override
        public void run() {
            final Object result = this.from.getResultPlain();
            try {
                if (result instanceof ExceptionResult exRes) {
                    this.to.complete(this.function.apply(exRes.ex));
                } else {
                    this.to.complete((T)unmaskNull(result));
                }
            } catch (final Throwable throwable) {
                this.failed(throwable);
            }
        }
    }

    private static final class UnparkTransform<T> extends Transform<Void, T> implements ForkJoinPool.ManagedBlocker {

        private volatile Thread thread;

        public UnparkTransform(final Completable<T> from, final Thread target) {
            super(null, from, null, null);
            this.thread = target;
        }

        @Override
        public void run() {
            final Thread t = this.thread;
            this.thread = null;
            LockSupport.unpark(t);
        }

        @Override
        public boolean block() throws InterruptedException {
            while (!this.isReleasable()) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                LockSupport.park(this);
            }

            return true;
        }

        @Override
        public boolean isReleasable() {
            return this.thread == null;
        }
    }
}
