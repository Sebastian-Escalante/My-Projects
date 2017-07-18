package cruftyKrab.ai.pathing;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.ikalagaming.logging.Logging;

import tiled.core.Map;

/**
 * A form of the A* pathfinding algorithm that allows for calculating a
 * semi-optimized path towards a point.
 *
 * It is based on the algorithm described in ''Near Optimal Hiearchial
 * Path-Finding'', a paper by Adi Botea, Martin Muller, and Jonathan Schaeffer
 * from the University of Alberta.
 *
 * Currently set up to only handle one level of clusters due to the very
 * specific (and small) map specs for this game, which will not change enough to
 * make it worth implementing multiple levels.
 *
 * @author Ches Burks
 *
 */
public class Pathfinding {

	/**
	 * True if you can go straight between the points without an intermediate
	 * step, which is not passed. To is two ahead of from. That is, one ahead of
	 * the skipped point, which is one ahead of from.
	 *
	 * @param from the first point
	 * @param to the next one
	 * @param map the map to test with
	 * @return true if a straight shot between points is possible
	 */
	private static boolean canSkipTo(Point from, Point to, PathingMap map) {
		final float m;
		final boolean horiz;

		if (to.x - from.x != 0) {
			m = (to.y - from.y) / (to.x - from.x);
			horiz = true;
		}
		else if (to.y - from.y != 0) {
			m = (to.x - from.x) / (to.y - from.y);
			horiz = false;
		}
		else {
			return true;// same point
		}

		final float b;

		if (horiz) {
			b = from.y - m * from.x;
		}
		else {
			b = from.x - m * from.y;
		}

		final int start;
		final int end;

		if (horiz) {
			start = from.x < to.x ? from.x : to.x;
			end = from.x < to.x ? to.x : from.x;

		}
		else {
			start = from.y < to.y ? from.y : to.y;
			end = from.y < to.y ? to.y : from.y;
		}

		int x, y;

		for (int i = start; i < end; ++i) {
			if (horiz) {
				x = i;
				y = Math.round(m * x + b);
			}
			else {
				y = i;
				x = Math.round(m * y + b);
			}
			if (!map.isWalkable(x, y)) {
				return false;
			}
		}
		return true;
	}

	private static BiPredicate<Point, Point> getConsumerForMap(PathingMap map) {
		Function<PathingMap, BiPredicate<Point, Point>> skipTest =
				(m) -> ((a, b) -> Pathfinding.canSkipTo(a, b, m));
		return skipTest.apply(map);
	}

	/**
	 * Arrays of clusters for fast access. This is set up as a list of columns,
	 * so it is arranged [x][y] and thus clusters[1][0] is the 0th element down
	 * the 1st column, and clusters [3] is the 3rd column.
	 */
	private Cluster clusters[][];

	private ReentrantLock clusterLock;

	private PathingMap map;

	private java.util.Map<Tuple, Path> pathCache;

	/**
	 * Creates a new class for pathfinding.
	 *
	 * @see Pathfinding#processMap(Map)
	 */
	public Pathfinding() {
		this.clusterLock = new ReentrantLock();
		this.pathCache =
				Collections.synchronizedMap(new HashMap<Tuple, Path>());
	}

	private void calcEntrancesBtwn(Cluster one, Cluster two) {
		final int oneXIndex, oneYIndex;
		final int twoXIndex, twoYIndex;

		final int dxIndex;
		final int dyIndex;

		this.clusterLock.lock();
		try {
			// check if they already have edges defined
			for (Edge e : one.externalEdges) {
				Cluster a = this.getCluster(e.node1.loc.x, e.node1.loc.y);
				Cluster b = this.getCluster(e.node2.loc.x, e.node2.loc.y);
				if (a.equals(one) && b.equals(two)) {
					return;
				}
				if (a.equals(two) && b.equals(one)) {
					return;
				}
			}

			oneXIndex = one.ulCorner.x / Cluster.CLUSTER_SIZE;
			oneYIndex = one.ulCorner.y / Cluster.CLUSTER_SIZE;
			twoXIndex = two.ulCorner.x / Cluster.CLUSTER_SIZE;
			twoYIndex = two.ulCorner.y / Cluster.CLUSTER_SIZE;

			dxIndex = Math.subtractExact(twoXIndex, oneXIndex);
			dyIndex = Math.subtractExact(twoYIndex, oneYIndex);

			if (Math.abs(dxIndex) > 1 || Math.abs(dyIndex) > 1) {
				Logging.finer("Pathfinding",
						"Calculating entrance between non-adjacent clusters");
				return;
			}
			if (Math.abs(dxIndex) == 1 && Math.abs(dyIndex) == 1) {
				Logging.finer("Pathfinding",
						"Calculating entrance between diagonal clusters");
				return;
			}

			if (dxIndex == 0 && dyIndex == 0) {
				Logging.finer("Pathfinding",
						"Calculating entrance between same cluster");
				return;
			}
			this.entranceCalcs(one, two, dxIndex, dyIndex);

		}
		finally {
			this.clusterLock.unlock();
		}
	}

	/**
	 * Calculate entrances between a cluster and clusters to each of the 4
	 * cardinal directions.
	 *
	 * @param c the cluster to calculate entrances for
	 * @param xIndex x index of the cluster in the cluster array
	 * @param yIndex y index of the cluster in the cluster array
	 */
	private void calculateEntrances(Cluster c, final int xIndex,
			final int yIndex) {
		Cluster n, e, s, w;
		this.clusterLock.lock();
		try {
			n = this.clusterAt(xIndex, yIndex - 1);
			e = this.clusterAt(xIndex + 1, yIndex);
			s = this.clusterAt(xIndex, yIndex + 1);
			w = this.clusterAt(xIndex - 1, yIndex);
		}
		finally {
			this.clusterLock.unlock();
		}
		if (n != null) {
			this.calcEntrancesBtwn(c, n);
		}
		if (e != null) {
			this.calcEntrancesBtwn(c, e);
		}
		if (s != null) {
			this.calcEntrancesBtwn(c, s);
		}
		if (w != null) {
			this.calcEntrancesBtwn(c, w);
		}
	}

	/**
	 * Should be called after calculating entrances, as it finds edges between
	 * entrances.
	 *
	 * @param c the cluster to calculate edges for
	 */
	private void calculateInernalEdges(Cluster c) {
		this.clusterLock.lock();
		try {
			final int size = c.entranceNodes.size();
			for (int i = 0; i < size; ++i) {
				for (int j = i + 1; j < size; ++j) {
					Edge e = new Edge(c.entranceNodes.get(i),
							c.entranceNodes.get(j), this.map);
					c.internalEdges.add(e);
					e.calcLength();
				}
			}
		}
		finally {
			this.clusterLock.unlock();
		}
	}

	/**
	 * Convenience for safely grabbing from the array at any index.
	 *
	 * @param xIndex x position
	 * @param yIndex y position
	 * @return the cluster at clusters[xIndex][yIndex] or null if invalid coords
	 */
	private Cluster clusterAt(final int xIndex, final int yIndex) {
		Cluster ret;
		this.clusterLock.lock();
		try {
			ret = this.clusters[xIndex][yIndex];
		}
		catch (@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e) {
			ret = null;
		}
		finally {
			this.clusterLock.unlock();
		}
		return ret;
	}

	private void entranceCalcs(Cluster one, Cluster two, final int dxIndex,
			final int dyIndex) {
		final int startX;
		final int startY;
		final int endX;
		final int endY;
		if (dyIndex == 0) {
			if (dxIndex == -1) {
				// two is to the left of one
				startY = one.ulCorner.y;
				endY = one.ulCorner.y + Cluster.CLUSTER_SIZE;
				startX = one.ulCorner.x;
				this.entrancesVert(startX, startY, endY, -1, one, two);
			}
			else if (dxIndex == 1) {
				// two is to the right of one
				startY = one.ulCorner.y;
				endY = one.ulCorner.y + Cluster.CLUSTER_SIZE;
				startX = one.ulCorner.x + Cluster.CLUSTER_SIZE - 1;
				this.entrancesVert(startX, startY, endY, 1, one, two);
			}
		}
		else if (dxIndex == 0) {
			if (dyIndex == -1) {
				// two is to the north of one
				startY = one.ulCorner.y;
				startX = one.ulCorner.x;
				endX = one.ulCorner.x + Cluster.CLUSTER_SIZE;
				this.entrancesHoriz(startY, startX, endX, -1, one, two);
			}
			else if (dyIndex == 1) {
				// two is to the south of one
				startY = one.ulCorner.y + Cluster.CLUSTER_SIZE - 1;
				startX = one.ulCorner.x;
				endX = one.ulCorner.x + Cluster.CLUSTER_SIZE;
				this.entrancesHoriz(startY, startX, endX, 1, one, two);
			}
		}
	}

	private void entrancesHoriz(final int y, final int startX, final int endX,
			final int dY, Cluster one, Cluster two) {
		// should be called from calcEntrances only
		int curStreak = 0;
		int curStart = startX;
		for (int i = startX; i < endX; ++i) {
			if (!this.map.isWalkable(i, y)) {
				if (curStreak > Cluster.ENTRANCE_WIDTH_CAP) {
					Node oneNodeOne = new Node(curStart, y, one);
					Node twoNodeOne = new Node(curStart, y + dY, two);
					Node oneNodeTwo = new Node(curStart + curStreak, y, one);
					Node twoNodeTwo =
							new Node(curStart + curStreak, y + dY, two);
					one.entranceNodes.add(oneNodeOne);
					one.entranceNodes.add(oneNodeTwo);
					two.entranceNodes.add(twoNodeOne);
					two.entranceNodes.add(twoNodeTwo);
					Edge edgeOne = new Edge(oneNodeOne, twoNodeOne, this.map);
					edgeOne.calcLength();
					Edge edgeTwo = new Edge(oneNodeTwo, twoNodeTwo, this.map);
					edgeTwo.calcLength();
					one.externalEdges.add(edgeOne);
					one.externalEdges.add(edgeTwo);
					two.externalEdges.add(edgeOne);
					two.externalEdges.add(edgeTwo);
				}
				else {
					Node oneNode = new Node((curStart + curStreak) / 2, y, one);
					Node twoNode =
							new Node((curStart + curStreak) / 2, y + dY, two);

					one.entranceNodes.add(oneNode);
					two.entranceNodes.add(twoNode);
					Edge edge = new Edge(oneNode, twoNode, this.map);
					edge.calcLength();
					one.externalEdges.add(edge);
					two.externalEdges.add(edge);
				}
				curStreak = 0;
				curStart = i + 1;
			}
			else {
				++curStreak;
			}
		}
	}

	private void entrancesVert(final int x, final int startY, final int endY,
			final int dX, Cluster one, Cluster two) {
		// should be called from calcEntrances only
		int curStreak = 0;
		int curStart = startY;
		for (int i = startY; i < endY; ++i) {
			if (!this.map.isWalkable(x, i)) {
				if (curStreak > Cluster.ENTRANCE_WIDTH_CAP) {
					Node oneNodeOne = new Node(x, curStart, one);
					Node twoNodeOne = new Node(x + dX, curStart, two);
					Node oneNodeTwo = new Node(x, curStart + curStreak, one);
					Node twoNodeTwo =
							new Node(x + dX, curStart + curStreak, two);
					one.entranceNodes.add(oneNodeOne);
					one.entranceNodes.add(oneNodeTwo);
					two.entranceNodes.add(twoNodeOne);
					two.entranceNodes.add(twoNodeTwo);
					Edge edgeOne = new Edge(oneNodeOne, twoNodeOne, this.map);
					edgeOne.calcLength();
					Edge edgeTwo = new Edge(oneNodeTwo, twoNodeTwo, this.map);
					edgeTwo.calcLength();
					one.externalEdges.add(edgeOne);
					one.externalEdges.add(edgeTwo);
					two.externalEdges.add(edgeOne);
					two.externalEdges.add(edgeTwo);
				}
				else {
					Node oneNode = new Node(x + curStreak,
							(curStart + curStreak) / 2, one);
					Node twoNode =
							new Node(x + dX, (curStart + curStreak) / 2, two);

					one.entranceNodes.add(oneNode);
					two.entranceNodes.add(twoNode);
					Edge edge = new Edge(oneNode, twoNode, this.map);
					edge.calcLength();
					one.externalEdges.add(edge);
					two.externalEdges.add(edge);
				}
				curStreak = 0;
				curStart = i + 1;
			}
			else {
				++curStreak;
			}
		}
	}

	/**
	 * Gets the closest node walkable to from the given point.
	 *
	 * @param to the point to start looking from
	 * @return the closest node, or null if none exist in the cluster
	 */
	private Node getClosest(Point to) {
		/*
		 * Set of nodes already evaluated
		 */
		HashSet<Point> closed = new HashSet<>();

		final Point start = new Point(to);
		/*
		 * The set of currently discovered nodes still to be evaluated.
		 * Initially, only the start node is known.
		 */
		ArrayList<Point> open = new ArrayList<>();
		open.add(start);

		while (!open.isEmpty()) {
			Point current = open.get(0);
			for (Cluster[] cols : this.clusters) {
				for (Cluster clust : cols) {
					for (Node node : clust.entranceNodes) {
						if (node.loc.equals(current)) {
							return node;
						}
					}
				}
			}
			open.remove(current);
			closed.add(current);
			for (Point neighbor : this.getNeigbors(current)) {
				if (closed.contains(neighbor)) {
					// Ignore any neighbor which is already evaluated.
					continue;
				}
				if (!open.contains(neighbor)) {
					open.add(neighbor);// discover a new node
					open.sort((a, b) -> Math.subtractExact(
							Math.round((int) a.distance(start)),
							Math.round((int) b.distance(start))));
				}
			}
		}
		Logging.warning("Pathfinding", "Could not find an entrance");
		return null;
	}

	/**
	 * Returns the cluster which contains the tile at (x, y).
	 *
	 * @param x the x position of the tile
	 * @param y the y position of the tile
	 * @return the cluster containing (x, y), or null if none exists.
	 */
	private Cluster getCluster(final int x, final int y) {
		if (x < 0 || y < 0) {
			return null;
		}
		if (x > this.map.getWidth() || y > this.map.getHeight()) {
			return null;
		}

		/*
		 * Which cluster is it, as the x index. The actual *COORDINATE* of the
		 * left tile would be (x / CLUSTER_SIZE)*CLUSTER_SIZE.
		 */
		final int xIndex = x / Cluster.CLUSTER_SIZE;

		/*
		 * Which cluster is it, as the y index. The actual *COORDINATE* of the
		 * left tile would be (y / CLUSTER_SIZE)*CLUSTER_SIZE.
		 */
		final int yIndex = y / Cluster.CLUSTER_SIZE;

		Cluster ret = null;
		this.clusterLock.lock();
		try {
			ret = this.clusters[xIndex][yIndex];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace(System.err);
			ret = null;
		}
		finally {
			this.clusterLock.unlock();
		}
		return ret;
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
	 * Returns the path from a given point to another. Caches intermediate paths
	 * (from set nodes to other set nodes), then tacks on paths to that from the
	 * given points.
	 *
	 * @param from the point to start at
	 * @param to the pont to end at
	 * @return the (non-smoothed) path between the points
	 */
	public Path getPath(Point from, Point to) {
		Node start = new Node(from.x, from.y, this.getCluster(from.x, from.y));
		Node end = new Node(to.x, to.y, this.getCluster(to.x, to.y));

		Node closeStart = this.getClosest(from);
		Node closeEnd = this.getClosest(to);

		Path cached = this.pathCache.get(new Tuple(closeStart, closeEnd));
		if (cached == null) {
			// try the reverse
			cached = this.pathCache.get(new Tuple(closeEnd, closeStart));
			if (cached != null) {
				Path p = cached.getInverted();
				Edge firstEdge = new Edge(start, closeStart, this.map, false);
				firstEdge.calcLength();
				Edge lastEdge = new Edge(closeEnd, end, this.map, false);
				lastEdge.calcLength();
				Path total = new Path();
				total.append(firstEdge.path);
				total.append(p);
				total.append(lastEdge.path);
				return total;
			}
		}

		// Edge firstEdge = new Edge(start, closeStart, map, false);
		// firstEdge.calcLength();
		// Edge lastEdge = new Edge(closeEnd, end, map, false);
		// lastEdge.calcLength();

		/*
		 * Set of nodes already evaluated
		 */
		HashSet<Node> closed = new HashSet<>();
		/*
		 * The set of currently discovered nodes still to be evaluated.
		 * Initially, only the start node is known.
		 */

		HashMap<Node, AStarData> data = new HashMap<>();

		ArrayList<Node> open = new ArrayList<>();

		open.add(closeStart);

		// generate map for cluster
		for (Cluster[] cols : this.clusters) {
			for (Cluster clust : cols) {
				for (Node n : clust.entranceNodes) {
					data.put(n, new AStarData(null, Integer.MAX_VALUE,
							Integer.MAX_VALUE));
				}
			}
		}
		data.put(closeStart,
				new AStarData(null, Integer.MAX_VALUE, Integer.MAX_VALUE));
		AStarData startData = data.get(closeStart);
		startData.cameFromPath = null;
		startData.gScore = 0;
		startData.fScore = (float) closeStart.loc.distance(closeEnd.loc);

		Path path = new Path();

		while (!open.isEmpty()) {
			Node current = open.get(0);
			if (current == end) {
				path = this.reconstructPath(current,
						data.get(current).cameFromPath, data);
				this.pathCache.put(new Tuple(closeStart, closeEnd), path);
				break;
			}
			open.remove(current);
			closed.add(current);

			for (Edge e : current.getEdges(0)) {
				Node other = null;
				if (e.node1.equals(current)) {
					other = e.node2;
				}
				else if (e.node2.equals(current)) {
					other = e.node1;
				}
				else {
					Logging.warning("Pathfinding",
							"Null node detected when calculating path.");
					continue;
				}
				if (closed.contains(other)) {
					// Ignore any neighbor which is already evaluated.
					continue;
				}

				float tentativeGScore =
						data.get(current).gScore + e.getLength();
				if (!open.contains(other)) {
					open.add(other);// discover a new node
					open.sort((a, b) -> Math.subtractExact(
							Math.round(data.get(a).fScore),
							Math.round(data.get(b).fScore)));
				}
				else if (tentativeGScore >= data.get(other).gScore) {
					continue;// this is not a better path
				}
				// this path is the best until now. Record it
				AStarData nDat = data.get(other);
				nDat.cameFromPath = e;
				nDat.gScore = tentativeGScore;
				// heuristic estimate
				nDat.fScore =
						nDat.gScore + (float) closeEnd.loc.distance(other.loc);
			}
		}

		Edge firstEdge = new Edge(start, closeStart, this.map, false);
		firstEdge.calcLength();
		Edge lastEdge = new Edge(closeEnd, end, this.map, false);
		lastEdge.calcLength();
		Path total = new Path();
		total.append(firstEdge.path);
		total.append(path);
		total.append(lastEdge.path);
		return total;
	}

	/**
	 * Process the given map and generate pathfinding graphs.
	 *
	 * @param m the map to process
	 */
	public void processMap(Map m) {
		Logging.finest("Pathfinding", "Beginning map processing...");
		this.clusterLock.lock();
		try {
			this.map = new PathingMap(m);

			// how many clusters can fit completely across the width
			int clusterWidth = this.map.getWidth() / Cluster.CLUSTER_SIZE;
			// how many clusters can fit completely across the width
			int clusterHeight = this.map.getHeight() / Cluster.CLUSTER_SIZE;
			this.clusters = new Cluster[clusterWidth][clusterHeight];

			this.splitIntoClusters();

			int count = 1;
			for (int i = 0; i < this.clusters.length; ++i) {
				final int len = this.clusters[i].length;
				for (int j = 0; j < len; ++j) {
					Logging.finest("Pathfinding",
							"Processing cluster " + count);
					++count;
					int xIndex = i / Cluster.CLUSTER_SIZE;
					int yIndex = j / Cluster.CLUSTER_SIZE;

					Cluster c = this.clusters[xIndex][yIndex];
					this.calculateEntrances(c, xIndex, yIndex);
					this.calculateInernalEdges(c);
				}
			}
		}
		finally {
			this.clusterLock.unlock();
		}
		Logging.finest("Pathfinding", "Finished processing map!");
	}

	private Path reconstructPath(final Node current, final Edge curEdge,
			final HashMap<Node, AStarData> data) {

		Path p = new Path();
		Edge prevEdge = curEdge;
		Node lastNode = current;
		ArrayDeque<Edge> reverseEdges = new ArrayDeque<>();

		// keep going until the start node
		while (prevEdge != null) {
			if (data.get(lastNode).cameFromPath == null) {
				break;
			}
			reverseEdges.push(data.get(lastNode).cameFromPath);
			// update last node so we can track what side of the edge we're on
			if (lastNode == data.get(lastNode).cameFromPath.node1) {
				lastNode = data.get(lastNode).cameFromPath.node2;
			}
			else {
				// assuming here
				lastNode = data.get(lastNode).cameFromPath.node1;
			}
			prevEdge = data.get(lastNode).cameFromPath;
		}

		for (Edge e; !reverseEdges.isEmpty();) {
			e = reverseEdges.pop();
			p.append(e.path);
		}
		return p;
	}

	/**
	 * Smooth the given path by removing unneeded points
	 *
	 * @param rough the path to smooth
	 */
	public void smoothPath(Path rough) {
		rough.smooth(Pathfinding.getConsumerForMap(this.map));
	}

	private void splitIntoClusters() {
		final int width = this.map.getWidth();
		final int height = this.map.getHeight();
		if (width % Cluster.CLUSTER_SIZE != 0
				|| height % Cluster.CLUSTER_SIZE != 0) {
			Logging.warning("Pathfinding",
					"map size not optimal for cluster size of "
							+ Cluster.CLUSTER_SIZE + " x "
							+ Cluster.CLUSTER_SIZE + ".");
		}

		for (int i = 0; i < width; i += Cluster.CLUSTER_SIZE) {
			for (int j = 0; j < height; j += Cluster.CLUSTER_SIZE) {
				Cluster c = new Cluster(i, j);
				this.clusters[i / Cluster.CLUSTER_SIZE][j
						/ Cluster.CLUSTER_SIZE] = c;
			}
		}
	}

}
