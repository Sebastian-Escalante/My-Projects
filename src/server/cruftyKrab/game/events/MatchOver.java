package cruftyKrab.game.events;

/**
 * Signals that a match is over.
 *
 * @author Ches Burks
 *
 */
public class MatchOver extends CancellableEvent {

	private final int matchID;

	/**
	 * A match over event for the given match ID.
	 *
	 * @param id the unique ID of the match.
	 */
	public MatchOver(final int id) {
		this.matchID = id;
	}

	/**
	 * Returns the match ID.
	 *
	 * @return the unique ID of the match
	 */
	public int getID() {
		return this.matchID;
	}
}
