package cruftyKrab.game.events;

import cruftyKrab.network.Connection;

/**
 * A player is attempting to join a match.
 *
 * @author Ches Burks
 *
 */
public class JoinMatch extends CancellableEvent {

	private final Connection sender;
	private final int matchID;

	/**
	 * Creates a {@link JoinMatch} event.
	 *
	 * @param id the ID of the match to join
	 * @param connection the connection that requested the join
	 */
	public JoinMatch(final int id, final Connection connection) {
		this.matchID = id;
		this.sender = connection;
	}

	/**
	 * Returns the match ID the player wants to join.
	 *
	 * @return the ID of the match to join
	 */
	public int getMatchID() {
		return this.matchID;
	}

	/**
	 * Returns the connection that requested the join
	 *
	 * @return the connection
	 */
	public Connection getSender() {
		return this.sender;
	}
}
