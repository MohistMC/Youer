package ca.spottedleaf.moonrise.patches.collisions.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;

public final class EmptyStreamForMoveCall<T> implements java.util.stream.Stream<T> {

    public static final ca.spottedleaf.moonrise.patches.collisions.util.EmptyStreamForMoveCall INSTANCE = new ca.spottedleaf.moonrise.patches.collisions.util.EmptyStreamForMoveCall();

    @Override
    public boolean noneMatch(java.util.function.Predicate<? super T> predicate) {
        return false; // important: ret false so the branch is never taken by mojang code
    }

    @Override
    public java.util.stream.Stream<T> filter(java.util.function.Predicate<? super T> predicate) {
        return null;
    }

    @Override
    public <R> java.util.stream.Stream<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
        return null;
    }

    @Override
    public java.util.stream.IntStream mapToInt(java.util.function.ToIntFunction<? super T> mapper) {
        return null;
    }

    @Override
    public java.util.stream.LongStream mapToLong(java.util.function.ToLongFunction<? super T> mapper) {
        return null;
    }

    @Override
    public java.util.stream.DoubleStream mapToDouble(java.util.function.ToDoubleFunction<? super T> mapper) {
        return null;
    }

    @Override
    public <R> java.util.stream.Stream<R> flatMap(java.util.function.Function<? super T, ? extends java.util.stream.Stream<? extends R>> mapper) {
        return null;
    }

    @Override
    public java.util.stream.IntStream flatMapToInt(java.util.function.Function<? super T, ? extends java.util.stream.IntStream> mapper) {
        return null;
    }

    @Override
    public java.util.stream.LongStream flatMapToLong(java.util.function.Function<? super T, ? extends java.util.stream.LongStream> mapper) {
        return null;
    }

    @Override
    public java.util.stream.DoubleStream flatMapToDouble(java.util.function.Function<? super T, ? extends java.util.stream.DoubleStream> mapper) {
        return null;
    }

    @Override
    public java.util.stream.Stream<T> distinct() {
        return null;
    }

    @Override
    public java.util.stream.Stream<T> sorted() {
        return null;
    }

    @Override
    public java.util.stream.Stream<T> sorted(java.util.Comparator<? super T> comparator) {
        return null;
    }

    @Override
    public java.util.stream.Stream<T> peek(java.util.function.Consumer<? super T> action) {
        return null;
    }

    @Override
    public java.util.stream.Stream<T> limit(long maxSize) {
        return null;
    }

    @Override
    public java.util.stream.Stream<T> skip(long n) {
        return null;
    }

    @Override
    public void forEach(java.util.function.Consumer<? super T> action) {

    }

    @Override
    public void forEachOrdered(java.util.function.Consumer<? super T> action) {

    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public <A> A[] toArray(java.util.function.IntFunction<A[]> generator) {
        return null;
    }

    @Override
    public T reduce(T identity, java.util.function.BinaryOperator<T> accumulator) {
        return null;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Optional<T> reduce(java.util.function.BinaryOperator<T> accumulator) {
        return java.util.Optional.empty();
    }

    @Override
    public <U> U reduce(U identity, java.util.function.BiFunction<U, ? super T, U> accumulator, java.util.function.BinaryOperator<U> combiner) {
        return null;
    }

    @Override
    public <R> R collect(java.util.function.Supplier<R> supplier, java.util.function.BiConsumer<R, ? super T> accumulator, java.util.function.BiConsumer<R, R> combiner) {
        return null;
    }

    @Override
    public <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector) {
        return null;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Optional<T> min(java.util.Comparator<? super T> comparator) {
        return java.util.Optional.empty();
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Optional<T> max(java.util.Comparator<? super T> comparator) {
        return java.util.Optional.empty();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public boolean anyMatch(java.util.function.Predicate<? super T> predicate) {
        return false;
    }

    @Override
    public boolean allMatch(java.util.function.Predicate<? super T> predicate) {
        return false;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Optional<T> findFirst() {
        return java.util.Optional.empty();
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Optional<T> findAny() {
        return java.util.Optional.empty();
    }


    @org.jetbrains.annotations.NotNull
    @Override
    public Iterator<T> iterator() {
        return null;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Spliterator<T> spliterator() {
        return null;
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Stream<T> sequential() {
        return null;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Stream<T> parallel() {
        return null;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Stream<T> unordered() {
        return null;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        return null;
    }

    @Override
    public void close() {

    }
}
