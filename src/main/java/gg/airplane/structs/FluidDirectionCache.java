package gg.airplane.structs;

import it.unimi.dsi.fastutil.HashCommon;

/**
 * This is a replacement for the cache used in FluidTypeFlowing.
 * The requirements for the previous cache were:
 *  - Store 200 entries
 *  - Look for the flag in the cache
 *  - If it exists, move to front of cache
 *  - If it doesn't exist, remove last entry in cache and insert in front
 *
 * This class accomplishes something similar, however has a few different
 * requirements put into place to make this more optimize:
 *
 *  - maxDistance is the most amount of entries to be checked, instead
 *    of having to check the entire list.
 *  - In combination with that, entries are all tracked by age and how
 *    frequently they're used. This enables us to remove old entries,
 *    without constantly shifting any around.
 *
 * Usage of the previous map would have to reset the head every single usage,
 * shifting the entire map. Here, nothing happens except an increment when
 * the cache is hit, and when it needs to replace an old element only a single
 * element is modified.
 */
public class FluidDirectionCache<T> {

    private static class FluidDirectionEntry<T> {
        private final T data;
        private final boolean flag;
        private int uses = 0;
        private int age = 0;

        private FluidDirectionEntry(T data, boolean flag) {
            this.data = data;
            this.flag = flag;
        }

        public int getValue() {
            return this.uses - (this.age >> 1); // age isn't as important as uses
        }

        public void incrementUses() {
            this.uses = this.uses + 1 & Integer.MAX_VALUE;
        }

        public void incrementAge() {
            this.age = this.age + 1 & Integer.MAX_VALUE;
        }
    }

    private final FluidDirectionEntry[] entries;
    private final int mask;
    private final int maxDistance; // the most amount of entries to check for a value

    public FluidDirectionCache(int size) {
        int arraySize = HashCommon.nextPowerOfTwo(size);
        this.entries = new FluidDirectionEntry[arraySize];
        this.mask = arraySize - 1;
        this.maxDistance = Math.min(arraySize, 4);
    }

    public Boolean getValue(T data) {
        FluidDirectionEntry curr;
        int pos;

        if ((curr = this.entries[pos = HashCommon.mix(data.hashCode()) & this.mask]) == null) {
            return null;
        } else if (data.equals(curr.data)) {
            curr.incrementUses();
            return curr.flag;
        }

        int checked = 1; // start at 1 because we already checked the first spot above

        while ((curr = this.entries[pos = (pos + 1) & this.mask]) != null) {
            if (data.equals(curr.data)) {
                curr.incrementUses();
                return curr.flag;
            } else if (++checked >= this.maxDistance) {
                break;
            }
        }

        return null;
    }

    public void putValue(T data, boolean flag) {
        FluidDirectionEntry<T> curr;
        int pos;

        if ((curr = this.entries[pos = HashCommon.mix(data.hashCode()) & this.mask]) == null) {
            this.entries[pos] = new FluidDirectionEntry<>(data, flag); // add
            return;
        } else if (data.equals(curr.data)) {
            curr.incrementUses();
            return;
        }

        int checked = 1; // start at 1 because we already checked the first spot above

        while ((curr = this.entries[pos = (pos + 1) & this.mask]) != null) {
            if (data.equals(curr.data)) {
                curr.incrementUses();
                return;
            } else if (++checked >= this.maxDistance) {
                this.forceAdd(data, flag);
                return;
            }
        }

        this.entries[pos] = new FluidDirectionEntry<>(data, flag); // add
    }

    private void forceAdd(T data, boolean flag) {
        int expectedPos = HashCommon.mix(data.hashCode()) & this.mask;

        int toRemovePos = expectedPos;
        FluidDirectionEntry entryToRemove = this.entries[toRemovePos];

        for (int i = expectedPos + 1; i < expectedPos + this.maxDistance; i++) {
            int pos = i & this.mask;
            FluidDirectionEntry entry = this.entries[pos];
            if (entry.getValue() < entryToRemove.getValue()) {
                toRemovePos = pos;
                entryToRemove = entry;
            }

            entry.incrementAge(); // use this as a mechanism to age the other entries
        }

        // remove the least used/oldest entry
        this.entries[toRemovePos] = new FluidDirectionEntry(data, flag);
    }
}
