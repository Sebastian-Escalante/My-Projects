package cruftyKrab.ai.pathing;

import java.awt.Point;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.ikalagaming.logging.Logging;

/**
 * A square on the map used for finding paths around the map.
 *
 * @author Ches Burks
 *
 */
public class Node {
	/**
	 * The coordinates of the node.
	 */
	protected Point loc;

	/**
	 * Edges with this as an endpoint.
	 */
	protected ArrayList<Edge> edges;

	/**
	 * The cluster this node is in.
	 */
	protected Cluster cluster;

	/**
	 * Create a node with given coordinates. These should be integers within the
	 * bounds of possible {@code short} values ({@link Short#MIN_VALUE} to
	 * {@link Short#MAX_VALUE}).
	 *
	 * @param xVal the x value, index starting at 0
	 * @param yVal the y value, index starting at 0
	 * @param clust The cluster this node is in
	 */
	public Node(final int xVal, final int yVal, Cluster clust) {
		int xValue = xVal;
		int yValue = yVal;
		if (xValue > Short.MAX_VALUE) {
			Logging.warning("Pathfinding",
					"Node x value would overflow, capping.");
			xValue = Short.MAX_VALUE;
		}
		if (yValue > Short.MAX_VALUE) {
			Logging.warning("Pathfinding",
					"Node y value would overflow, capping.");
			yValue = Short.MAX_VALUE;
		}
		this.loc = new Point(xValue, yValue);
		this.edges = new ArrayList<>();
		this.cluster = clust;
	}

	/**
	 * Returns edges of a certain level.
	 *
	 * @param level the level of edge to return
	 * @return a new ArrayList containing edges with specified level
	 */
	protected ArrayList<Edge> getEdges(final int level) {
		final byte l;
		if (level > Byte.MAX_VALUE) {
			l = Byte.MAX_VALUE;
		}
		else {
			l = (byte) level;
		}
		return new ArrayList<>(this.edges.stream().filter(e -> (e.level == l))
				.collect(Collectors.toList()));
	}

	/**
	 * Returns an int that has the first 16 bits as the x value and the last 16
	 * as the y value. Sort of a hash function of the position.
	 *
	 * @return a value based on the position of the node
	 */
	public int value() {
		return this.loc.hashCode();
	}

}
