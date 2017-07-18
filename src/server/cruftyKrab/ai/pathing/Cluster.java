package cruftyKrab.ai.pathing;

import java.awt.Point;
import java.util.ArrayList;

import com.ikalagaming.logging.Logging;

/**
 * A disjunct rectangular area of a map.
 *
 * These are only one level, as the map is too small to warrant more, and the
 * size will not change during the scope of the project.
 *
 * @author Ches Burks
 *
 */
public class Cluster {

	/**
	 * Defines the upper limit of entrance size for creating one transition for
	 * an entrance at the center. Entrances wider than this will have two
	 * transitions, one at each end.
	 */
	public static final int ENTRANCE_WIDTH_CAP = 6;

	/*
	 * Defines the number of (l-1)-clusters that are grouped together in an
	 * {@code n * n} area to form a l-cluster.
	 *
	 * public static final int CLUSTER_GROUPING = 2;
	 */

	/**
	 * The size of the smallest cluster. That is, l-clusters of level 0.
	 */
	public static final int CLUSTER_SIZE = 10;

	/**
	 * Edges between nodes inside this cluster
	 */
	protected ArrayList<Edge> internalEdges;
	/**
	 * Edges between nodes in this cluster and nodes in other clusters.
	 */
	protected ArrayList<Edge> externalEdges;

	/**
	 * Nodes along the edge of the cluster, at entrances.
	 */
	protected ArrayList<Node> entranceNodes;

	/**
	 * The upper left corner of the cluster.
	 */
	protected Point ulCorner;

	/**
	 * Creates a new cluster at the given level. Level 0 is the level tiles are
	 * on, and higher is l-clusters above that.
	 *
	 * @param xPos the x position of the top left corner of the cluster
	 * @param yPos the y position of the top left corner of the cluster
	 */
	public Cluster(final int xPos, final int yPos) {
		this.internalEdges = new ArrayList<>();
		this.externalEdges = new ArrayList<>();
		this.entranceNodes = new ArrayList<>();

		int xValue = xPos;
		int yValue = yPos;
		if (xValue > Short.MAX_VALUE) {
			Logging.warning("Pathfinding",
					"Cluster x value would overflow, capping.");
			xValue = Short.MAX_VALUE;
		}
		if (yValue > Short.MAX_VALUE) {
			Logging.warning("Pathfinding",
					"Cluster y value would overflow, capping.");
			yValue = Short.MAX_VALUE;
		}
		this.ulCorner = new Point(xValue, yValue);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Cluster)) {
			return false;
		}
		Cluster o = (Cluster) other;
		return this.ulCorner.equals(o.ulCorner);
	}

	@Override
	public int hashCode() {
		return this.ulCorner.hashCode();
	}

}
