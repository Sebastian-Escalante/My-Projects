package cruftyKrab.network.messages.out;

/**
 * A list of players in a game.
 *
 * @author Ches Burks
 *
 */
public class PlayerList {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "PlayerList";
	/**
	 * How many players are in the player list.
	 */
	public double entityCount;
	/**
	 * The list of players in a game.
	 */
	public EntitySpawned[] entities;
}
