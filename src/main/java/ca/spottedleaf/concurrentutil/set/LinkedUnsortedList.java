package ca.spottedleaf.concurrentutil.set;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class LinkedUnsortedList<E> implements Iterable<E> {

    private Link<E> head;
    private Link<E> tail;

    public LinkedUnsortedList() {}

    public void clear() {
        this.head = this.tail = null;
    }

    public boolean isEmpty() {
        return this.head == null;
    }

    public E first() {
        final Link<E> head = this.head;
        return head == null ? null : head.element;
    }

    public E last() {
        final Link<E> tail = this.tail;
        return tail == null ? null : tail.element;
    }

    public boolean containsFirst(final E element) {
        for (Link<E> curr = this.head; curr != null; curr = curr.next) {
            if (Objects.equals(element, curr.element)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsLast(final E element) {
        for (Link<E> curr = this.tail; curr != null; curr = curr.prev) {
            if (Objects.equals(element, curr.element)) {
                return true;
            }
        }
        return false;
    }

    private void removeNode(final Link<E> node) {
        final Link<E> prev = node.prev;
        final Link<E> next = node.next;

        // help GC
        node.element = null;
        node.prev = null;
        node.next = null;

        if (prev == null) {
            this.head = next;
        } else {
            prev.next = next;
        }

        if (next == null) {
            this.tail = prev;
        } else {
            next.prev = prev;
        }
    }

    public boolean remove(final Link<E> link) {
        if (link.element == null) {
            return false;
        }

        this.removeNode(link);
        return true;
    }

    public boolean removeFirst(final E element) {
        for (Link<E> curr = this.head; curr != null; curr = curr.next) {
            if (Objects.equals(element, curr.element)) {
                this.removeNode(curr);
                return true;
            }
        }
        return false;
    }

    public boolean removeLast(final E element) {
        for (Link<E> curr = this.tail; curr != null; curr = curr.prev) {
            if (Objects.equals(element, curr.element)) {
                this.removeNode(curr);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private Link<E> next = LinkedUnsortedList.this.head;

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            @Override
            public E next() {
                final Link<E> next = this.next;
                if (next == null) {
                    throw new NoSuchElementException();
                }
                this.next = next.next;
                return next.element;
            }
        };
    }

    public E pollFirst() {
        final Link<E> head = this.head;
        if (head == null) {
            return null;
        }

        final E ret = head.element;
        final Link<E> next = head.next;

        // unlink head
        this.head = next;
        if (next == null) {
            this.tail = null;
        } else {
            next.prev = null;
        }

        // help GC
        head.element = null;
        head.next = null;

        return ret;
    }

    public E pollLast() {
        final Link<E> tail = this.tail;
        if (tail == null) {
            return null;
        }

        final E ret = tail.element;
        final Link<E> prev = tail.prev;

        // unlink tail
        this.tail = prev;
        if (prev == null) {
            this.head = null;
        } else {
            prev.next = null;
        }

        // help GC
        tail.element = null;
        tail.prev = null;

        return ret;
    }

    public Link<E> addLast(final E element) {
        final Link<E> curr = this.tail;
        if (curr != null) {
            return this.tail = new Link<>(element, curr, null);
        } else {
            return this.head = this.tail = new Link<>(element);
        }
    }

    public Link<E> addFirst(final E element) {
        final Link<E> curr = this.head;
        if (curr != null) {
            return this.head = new Link<>(element, null, curr);
        } else {
            return this.head = this.tail = new Link<>(element);
        }
    }

    public static final class Link<E> {
        private E element;
        private Link<E> prev;
        private Link<E> next;

        private Link(final E element) {
            this.element = element;
        }

        private Link(final E element, final Link<E> prev, final Link<E> next) {
            this.element = element;
            this.prev = prev;
            this.next = next;
        }
    }
}
