package cruftyKrab.ai.pathing;

import java.awt.Point;

/**
 * Data stored for nodes/tiles in the a star pathfinding.
 *
 * @author Ches Burks
 *
 */
public class AStarData implements Comparable<AStarData> {
	/**
	 * For each node, which node it can most efficiently be reached from. If a
	 * node can be reached from many nodes, cameFrom will eventually contain the
	 * most efficient previous step.
	 */
	public Point cameFrom;
	/**
	 * For each node, which node it can most efficiently be reached from. If a
	 * node can be reached from many nodes, cameFrom will eventually contain the
	 * most efficient previous step.
	 */
	public Edge cameFromPath;
	/**
	 * For each node, the cost of getting from the start node to that node.
	 */
	public float gScore;
	/**
	 * For each node, the total cost of getting from the start node to the goal
	 * by passing by that node. That value is partly known, partly heuristic.
	 */
	public float fScore;

	/**
	 * Creates a new AStarData
	 *
	 * @param bestFrom the node it can best be reached from
	 * @param g the cost to this node
	 * @param f guessed cost to end node
	 */
	public AStarData(Point bestFrom, int g, int f) {
		this.cameFrom = bestFrom;
		this.gScore = g;
		this.fScore = f;
	}

	@Override
	public int compareTo(AStarData o) {
		return Math.subtractExact(Math.round(this.fScore),
				Math.round(o.fScore));
	}
}
