package alternate.current.wire;

/**
 * A WireConnection represents a connection between two WireNodes. The connection
 * can be one-way (only one wire can provide power to the other) or two-way.
 * 
 * @author Space Walker
 */
class WireConnection {

    /** The connected wire. */
    final WireNode wire;
    /** The cardinal direction from the owning wire to the connected wire. */
    final int iDir;
    /** Whether the owning wire can provide power TO the connected wire. */
    final boolean offer;
    /** Whether the connected wire can provide power TO the owning wire. */
    final boolean accept;
    /** The next connection in the linked list. */
    WireConnection next;

    WireConnection(WireNode wire, int iDir, boolean offer, boolean accept) {
        this.wire = wire;
        this.iDir = iDir;
        this.offer = offer;
        this.accept = accept;
    }
}
