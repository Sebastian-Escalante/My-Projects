package cruftyKrab.game.events;

import cruftyKrab.game.lobby.Match;

/**
 * A new match was created by a player.
 *
 * @author Ches Burks
 *
 */
public class MatchCreated extends CancellableEvent {

	private Match match;

	/**
	 * Creates a new event for the given match.
	 *
	 * @param newMatch the match that was created.
	 */
	public MatchCreated(Match newMatch) {
		this.match = newMatch;
	}

	/**
	 * Returns a reference to the newly created match.
	 *
	 * @return the match that has been created.
	 */
	public Match getMatch() {
		return this.match;
	}
}
