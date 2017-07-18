package cruftyKrab.network.messages;

/**
 * Info about a match.
 *
 * @author Ches Burks
 *
 */
public class MatchInfo {
	/**
	 * The state for waiting to start rounds. ({@value} ).
	 */
	public static final String WAITING = "Waiting";
	/**
	 * The state for a match that is already started. ({@value} ).
	 */
	public static final String STARTED = "Started";

	/**
	 * The unique ID of the match.
	 */
	public double matchID;
	/**
	 * The current number of players in the match.
	 */
	public double curPlayers;
	/**
	 * The maximum number of players in the match.
	 */
	public double maxPlayers;
	/**
	 * The username of who created the match.
	 */
	public String host;
	/**
	 * The state of the game, either {@link #WAITING} or {@link #STARTED}.
	 */
	public String state;
}
