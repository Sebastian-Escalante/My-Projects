package cruftyKrab.ai.pathing;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;

import com.ikalagaming.logging.Logging;

/**
 * A list of tiles that represent the steps from one point to another.
 *
 * @author Ches Burks
 *
 */
public class Path {

	private int length;

	private ArrayList<Point> tiles;
	private ReentrantLock dataLock;

	/**
	 * Creates a new empty path.
	 */
	public Path() {
		this.length = 0;
		this.tiles = new ArrayList<>();
		this.dataLock = new ReentrantLock();
	}

	/**
	 * Makes a copy of nextTile and tacks it onto the end of the path. If it is
	 * not within one tile of the end, it is ignored.
	 *
	 * @param nextTile the next step in the path.
	 */
	public void addStep(final Point nextTile) {
		if (nextTile == null) {
			return;
		}
		Point copy = new Point(nextTile);

		this.dataLock.lock();
		try {
			if (this.tiles.isEmpty()) {
				this.tiles.add(copy);
			}
			else {
				Point last = this.tiles.get(this.tiles.size() - 1);
				int dx = Math.abs(Math.subtractExact(copy.x, last.x));
				int dy = Math.abs(Math.subtractExact(copy.y, last.y));

				if (dx == 0 && dy == 0) {
					Logging.finer("Pathfinding",
							"Could not add step to path. Same as endpoint.");
				}
				else if (dx > 1 || dy > 1) {
					Logging.finer("Pathfinding",
							"Could not add step to path. Too far away.");
				}
				else {
					this.tiles.add(copy);
					if (dx == 0 || dy == 0) {
						this.length += Edge.STRAIGHT_DIST;
					}
					else {
						this.length += Edge.DIAGONAL_DIST;
					}
				}
			}
		}
		catch (ArithmeticException e) {
			e.printStackTrace(System.err);
			Logging.finer("Pathfinding", "Error adding step to path.");
		}
		finally {
			this.dataLock.unlock();
		}
	}

	/**
	 * Adds a step to the end, trusting that it can be added, and not
	 * calculating or updating path length.
	 *
	 * @param nextTile the tile to add
	 */
	private void addStepBlindly(final Point nextTile) {
		if (nextTile == null) {
			return;
		}
		Point copy = new Point(nextTile);

		this.dataLock.lock();
		try {
			if (this.tiles.isEmpty()) {
				this.tiles.add(copy);
			}
			else {
				this.tiles.add(copy);
			}
		}
		finally {
			this.dataLock.unlock();
		}
	}

	/**
	 * Adds all the steps from the other path to the end of this path. This
	 * tries to minimize calculations in adding the two paths.
	 *
	 * @param other the path to append to the end of this one
	 */
	public void append(Path other) {
		this.dataLock.lock();
		other.dataLock.lock();
		try {
			Point start = other.getStart();
			if (start == null) {
				Logging.finest("Path", "Path start is null");
				/*
				 * System.out.println(
				 * com.ikalagaming.util.ArrayOperations.convertToString(
				 * Thread.currentThread().getStackTrace()));
				 */

				return;
			}
			// if the start and end are the same
			if (start.equals(this.getEnd()) && other.tiles.size() >= 2) {
				// then just tack on the others
				for (int i = 1; i < other.tiles.size(); ++i) {
					this.addStepBlindly(other.tiles.get(i));
				}
				// add the length as the adding does not calculate it for you
				this.length += other.length;
			}
			else {
				this.appendDiffStart(other);
			}
		}
		finally {
			// Order is very important. Exactly reverse of locking order.
			other.dataLock.unlock();
			this.dataLock.unlock();
		}
	}

	private void appendDiffStart(Path other) {
		// the two paths don't intersect at the end
		Point last = this.getEnd();
		Point oStart = other.getStart();
		int dx = Math.abs(Math.subtractExact(oStart.x, last.x));
		int dy = Math.abs(Math.subtractExact(oStart.y, last.y));
		if (dx > 1 || dy > 1) {
			Logging.finer("Pathfinding",
					"Could not add step to path. Too far away.");
		}
		else {
			// add the first step normally
			this.addStep(oStart);
			// then add the rest blindly and update the length
			if (other.tiles.size() >= 2) {
				for (int i = 1; i < other.tiles.size(); ++i) {
					this.addStepBlindly(other.tiles.get(i));
				}
				/*
				 * the full length is added, + the length added by connecting
				 * the paths (that part is done in addStep)
				 */
				this.length += other.length;
			}
		}
	}

	/**
	 * Returns a copy of the end point, or null if this path is empty.
	 *
	 * @return the last point in the path, or null if empty.
	 */
	public Point getEnd() {
		Point ret;
		this.dataLock.lock();
		try {
			if (this.tiles.isEmpty()) {
				ret = null;
			}
			else {
				ret = new Point(this.tiles.get(this.tiles.size() - 1));
			}
		}
		finally {
			this.dataLock.unlock();
		}

		return ret;
	}

	/**
	 * Returns a copy of this path, but in reverse order.
	 *
	 * @return the path, but with an inverted list of tiles.
	 */
	public Path getInverted() {
		Path p = new Path();
		this.dataLock.lock();
		p.dataLock.lock();
		try {
			// copy of tiles, but reversed
			for (int i = this.tiles.size() - 1; i >= 0; --i) {
				p.tiles.add(new Point(this.tiles.get(i)));
			}
			p.length = this.length;
		}
		finally {
			// Order is very important. Exactly reverse of locking order.
			p.dataLock.unlock();
			this.dataLock.unlock();
		}

		return p;
	}

	/**
	 * Returns a copy of the start point, or null if this path is empty.
	 *
	 * @return the first point in the path, or null if empty.
	 */
	public Point getStart() {
		Point ret;
		this.dataLock.lock();
		try {
			if (this.tiles.isEmpty()) {
				ret = null;
			}
			else {
				ret = new Point(this.tiles.get(0));
			}
		}
		finally {
			this.dataLock.unlock();
		}

		return ret;
	}

	/**
	 * Returns a deep copy of the array of tiles. Modifying the list or its
	 * contents will not affect the path.
	 *
	 * @return a copy of the (ordered) list of tile locations
	 */
	public ArrayList<Point> getTiles() {
		ArrayList<Point> ret = new ArrayList<>();
		for (int i = 0; i < this.tiles.size(); ++i) {
			ret.add(new Point(this.tiles.get(i)));
		}
		return ret;
	}

	/**
	 * How long the path is in units of distance.
	 *
	 * @return the distance to travel the whole path.
	 */
	public int length() {
		int ret = -1;
		this.dataLock.lock();
		try {
			ret = this.length;
		}
		finally {
			this.dataLock.unlock();
		}
		return ret;
	}

	/**
	 * Smoothes out the path using a predicate function passed two points passed
	 * X, Z out of every (X, Y, Z) group of elements.
	 *
	 * @param tester the function to test with
	 */
	protected void smooth(BiPredicate<Point, Point> tester) {
		this.dataLock.lock();
		try {
			for (int i = 0; i < this.tiles.size(); ++i) {
				if (i + 2 > this.tiles.size()) {
					break;
				}
				if (tester.test(this.tiles.get(i), this.tiles.get(i + 2))) {
					this.tiles.remove(i + 1);
					--i;
				}
			}
			this.length = Integer.MAX_VALUE;
		}
		finally {
			this.dataLock.unlock();
		}

	}

	/**
	 * The number of steps or tiles in this path.
	 *
	 * @return the number of steps in the whole path
	 */
	public int steps() {
		int ret = -1;
		this.dataLock.lock();
		try {
			ret = this.tiles.size() - 1;
		}
		finally {
			this.dataLock.unlock();
		}
		return ret;
	}

}
