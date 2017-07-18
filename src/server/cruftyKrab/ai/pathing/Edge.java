package cruftyKrab.ai.pathing;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ikalagaming.logging.Logging;

/**
 * A line between two Nodes.
 *
 * @author Ches Burks
 *
 */
public class Edge implements Comparable<Edge> {
	/**
	 * Distance between adjacent nodes to the 4 sides
	 */
	public static final float STRAIGHT_DIST = 1;
	/**
	 * Distance between nodes adjacent to the 4 diagonal directions
	 */
	public static final float DIAGONAL_DIST = (float) Math.sqrt(2);

	// int n for clusters of [n x n] subclusters

	/**
	 * Returns the (positive) distance between neighbors, assuming they are
	 * adjacent. If they aren't right next to each other, the distance as a
	 * straight shot between the two is returned.
	 *
	 * @param a the first point
	 * @param b the second point
	 * @return the distance, or distance as the crow flies otherwise. 0 if
	 *         either point is null.
	 */
	protected static float getDist(Point a, Point b) {
		if (a == null || b == null) {
			return 0;
		}
		final int dxAbs = Math.abs(Math.subtractExact(a.x, b.x));
		final int dyAbs = Math.abs(Math.subtractExact(a.y, b.y));
		if (dxAbs < 0 || dxAbs > 1 || dyAbs < 0 || dyAbs > 1) {
			// not adjacent
			return (float) a.distance(b);
		}
		if (dxAbs == 0 && dyAbs == 0) {
			return 0;
		}
		if (dxAbs + dyAbs == 1) {
			// dx = +-1 and dy = 0, or dx = 0 and dy = +-1
			return Edge.STRAIGHT_DIST;
		}
		if (dxAbs == 1 && dyAbs == 1) {
			return Edge.DIAGONAL_DIST;
		}
		// fall back to linear distance
		return (float) a.distance(b);
	}

	/**
	 * The first end node of the edge.
	 */
	protected Node node1;

	/**
	 * The second end node of the edge.
	 */
	protected Node node2;

	/**
	 * The actual path from node1 to node2
	 */
	protected Path path;

	private int length;

	private PathingMap map;

	/**
	 * The level of the edge. Edge level 0 is the lowest level.
	 */
	protected byte level;

	/**
	 * Constructs a new edge, given two nodes that represent the end points.
	 * Defaults to inserting into the map.
	 *
	 * @param one the first node
	 * @param two the second node
	 * @param m the map this edge is on
	 * @see #Edge(Node, Node, PathingMap, boolean)
	 */
	public Edge(Node one, Node two, PathingMap m) {
		this(one, two, m, true);
	}

	/**
	 * Constructs a new edge, given two nodes that represent the end points.
	 *
	 * @param one the first node
	 * @param two the second node
	 * @param m the map this edge is on
	 * @param insert true if you want to insert into the map, false to leave
	 *            nodes untouched.
	 */
	public Edge(Node one, Node two, PathingMap m, boolean insert) {
		this.map = m;
		this.node1 = one;
		this.node2 = two;
		this.level = 0;
		if (insert) {
			this.node1.edges.add(this);
			this.node2.edges.add(this);
		}
		this.length = Integer.MIN_VALUE;
		this.path = new Path();
	}

	/**
	 * Calculates the length of the edge if it is not known, otherwise return
	 * the pre-calculated value.
	 *
	 * @return the optimal length from one end to the other
	 */
	public int calcLength() {
		if (this.length == Integer.MIN_VALUE) {
			// a* with graph or lower level edges
			if (this.level == 0) {
				int len = this.calcLengthTiles();
				if (len == -1) {
					Logging.warning("Pathfinding",
							"There was a problem calculating a path (Inter-cluster pathing not currently allowed).");
					this.length = Integer.MAX_VALUE;
				}
				else {
					this.length = len;
				}
			}
			if (this.level > 0) {
				Logging.warning("Pathfinding",
						"Only edge level 1 is currently supported.");
			}
		}

		return this.length;
	}

	/**
	 * Returns the length of the path between the two points. If the path does
	 * not exist, it calculates the path between the points and records it
	 * locally. If the paths are in different clusters (currently not
	 * supported), returns -1 and does not calculate a path. If the nodes aren't
	 * connected, returns INT_MAX.
	 *
	 * @return the length of the path along this edge, -1 if it is between
	 *         clusters, INT_MAX if the node could not be reached.
	 */
	protected int calcLengthTiles() {
		if (!this.node1.cluster.equals(this.node2.cluster)) {
			float dist = Edge.getDist(this.node1.loc, this.node2.loc);
			if (Math.round(dist) == 1) {
				Path p = new Path();
				p.addStep(this.node1.loc);
				p.addStep(this.node2.loc);
				this.path = p;
				this.length = 1;
				if (p.length() != 1) {
					Logging.warning("Pathfinding",
							"Intra-cluster path of length " + p.length()
									+ " is not 1.");
				}
				return this.length;
			}
			Logging.warning("Pathfinding",
					"A star outside clusters is currently not allowed.");
			return -1;
		}

		/*
		 * Set of nodes already evaluated
		 */
		HashSet<Point> closed = new HashSet<>();
		/*
		 * The set of currently discovered nodes still to be evaluated.
		 * Initially, only the start node is known.
		 */

		HashMap<Point, AStarData> data = new HashMap<>();
		ArrayList<Point> open = new ArrayList<>();
		// SortedSet<Point> open = new TreeSet<>(
		// (a, b) -> Math.subtractExact(Math.round(data.get(a).fScore),
		// Math.round(data.get(b).fScore)));

		Point start = new Point(this.node1.loc);
		open.add(start);

		Point end = new Point(this.node2.loc);

		Cluster loc = this.node1.cluster;

		// generate map for cluster
		for (int i = loc.ulCorner.x; i < Cluster.CLUSTER_SIZE; ++i) {
			for (int j = loc.ulCorner.y; j < Cluster.CLUSTER_SIZE; ++j) {
				data.put(new Point(i, j), new AStarData(null, Integer.MAX_VALUE,
						Integer.MAX_VALUE));
			}
		}

		AStarData startData = data.get(start);
		startData.cameFrom = start;
		startData.gScore = 0;
		startData.fScore = this.heuristicCostEstimate(start, end);

		while (!open.isEmpty()) {
			Point current = open.get(0);
			if (current.equals(end)) {
				this.path = this.reconstructPath(current, data);
				return this.path.length();
			}
			open.remove(current);
			closed.add(current);
			for (Point neighbor : this.getNeigbors(current)) {
				if (closed.contains(neighbor)) {
					// Ignore any neighbor which is already evaluated.
					continue;
				}
				if (!data.containsKey(neighbor)) {
					data.put(new Point(neighbor.x, neighbor.y), new AStarData(
							null, Integer.MAX_VALUE, Integer.MAX_VALUE));
				}

				float tentativeGScore = data.get(current).gScore
						+ Edge.getDist(current, neighbor);
				if (!open.contains(neighbor)) {
					open.add(neighbor);// discover a new node
					open.sort((a, b) -> Math.subtractExact(
							Math.round(data.get(a).fScore),
							Math.round(data.get(b).fScore)));
				}
				else if (tentativeGScore >= data.get(neighbor).gScore) {
					continue;// this is not a better path
				}
				// this path is the best until now. Record it
				AStarData nDat = data.get(neighbor);
				if (nDat == null) {
					continue;
				}
				nDat.cameFrom = current;
				nDat.gScore = tentativeGScore;
				nDat.fScore =
						nDat.gScore + this.heuristicCostEstimate(neighbor, end);
			}
		}

		return Integer.MAX_VALUE;
	}

	/**
	 * Returns 0 if they have the same 2 nodes in any order, or the relative
	 * values if not. Should follow all specifications defined in the interface.
	 */
	@Override
	public int compareTo(Edge o) {
		if (o == null) {
			throw new NullPointerException();
		}
		if (o.node1 == this.node1 && o.node2 == this.node2) {
			return 0;
		}
		if (o.node2 == this.node1 && o.node1 == this.node2) {
			return 0;
		}
		if (o.node1 == this.node1) {
			return o.node2.value() - this.node2.value();
		}
		if (o.node1 == this.node2) {
			return o.node2.value() - this.node1.value();
		}
		if (o.node2 == this.node2) {
			return o.node1.value() - this.node1.value();
		}
		if (o.node2 == this.node1) {
			return o.node1.value() - this.node2.value();
		}
		return (o.node1.value() + o.node2.value())
				- (this.node1.value() + this.node2.value());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Edge)) {
			return super.equals(obj);
		}
		try {
			Edge e2 = (Edge) obj;
			int res = this.compareTo(e2);
			return res == 0;
		}
		catch (@SuppressWarnings("unused") Exception e) {
			return false;
		}

	}

	/**
	 * Returns the length of the edge
	 *
	 * @return the distance from one node to the other
	 */
	public int getLength() {
		return this.calcLength();
	}

	/**
	 * Returns a list of walkable neighbors of a point.
	 *
	 * @param of the point to find neighbors of
	 * @return A set of points, sorted from lowest fScore to highest.
	 */
	private Set<Point> getNeigbors(final Point of) {
		Set<Point> ret = new HashSet<>();
		// Add points around of that are walkable (non-existent points aren't)
		int i, j;
		int x, y;
		for (i = -1; i <= 1; ++i) {
			for (j = -1; j <= 1; ++j) {
				if (j == 0 && i == 0) {
					continue;
				}
				x = Math.addExact(of.x, i);
				y = Math.addExact(of.y, j);

				if (this.map.isWalkable(x, y)) {
					ret.add(new Point(x, y));
				}
			}
		}
		return ret;
	}

	/**
	 * Guess distance, that is, straight up distance as the crow flies between
	 * the points.
	 *
	 * @param from the first point
	 * @param to the second point
	 * @return the distance as a float
	 */
	protected float heuristicCostEstimate(final Point from, final Point to) {
		return (float) from.distance(to);
	}

	private Path reconstructPath(final Point current,
			final HashMap<Point, AStarData> data) {
		Path p = new Path();
		Point prevStep = current;
		// keep going until the start node
		while (data.get(prevStep).cameFrom != prevStep) {
			p.addStep(prevStep);
			prevStep = data.get(prevStep).cameFrom;
		}
		return p.getInverted();
	}

}
