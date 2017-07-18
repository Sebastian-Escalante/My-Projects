package cruftyKrab.network.messages.out;

import cruftyKrab.network.messages.MoveInfo;

/**
 * A list of moves that has happened recently.
 *
 * @author Ches Burks
 *
 */
public class MoveSet {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "MoveSet";
	/**
	 * How many moves are in the move list.
	 */
	public double moveCount;
	/**
	 * The list of moves since last update.
	 */
	public MoveInfo[] moves;
}
