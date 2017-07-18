package cruftyKrab.network.messages.in;

/**
 * A client wants to move their mascot.
 *
 * @author Ches Burks
 *
 */
public class PosUpdate {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "posUpdate";
	/**
	 * The x position.
	 */
	public double xPosition;
	/**
	 * The y position.
	 */
	public double yPosition;
	/**
	 * The way the player is facing.
	 */
	public String facing;
}
