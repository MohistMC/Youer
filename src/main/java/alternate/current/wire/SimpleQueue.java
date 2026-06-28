package alternate.current.wire;

import java.util.AbstractQueue;
import java.util.Iterator;

public class SimpleQueue extends AbstractQueue<WireNode> {

    private WireNode head;
    private WireNode tail;

    private int size;

    SimpleQueue() {

    }

    @Override
    public boolean offer(WireNode node) {
        if (node == null) {
            throw new NullPointerException();
        }

        if (head == null) {
            head = tail = node;
        } else {
            tail.next_wire = node;
            tail = node;
        }

        size++;

        return true;
    }

    @Override
    public WireNode poll() {
        if (head == null) {
            return null;
        }

        WireNode node = head;
        WireNode next = node.next_wire;

        if (next == null) {
            clear();
        } else {
            node.next_wire = null;
            head = next;
            size--;
        }

        return node;
    }

    @Override
    public WireNode peek() {
        return head;
    }

    @Override
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public Iterator<WireNode> iterator() {
        return new SimpleIterator();
    }

    @Override
    public int size() {
        return size;
    }

    private class SimpleIterator implements Iterator<WireNode> {

        private WireNode node;

        private SimpleIterator() {
            this.node = head;
        }

        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public WireNode next() {
            WireNode wire = node;
            node = node.next_wire;
            return wire;
        }
    }
}
