package cruftyKrab.network.messages.out;

import cruftyKrab.game.MascotColor;

/**
 * A client has connected, inform them of their ID and color.
 *
 * @author Ches Burks
 *
 */
public class Welcome {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "Welcome";
	/**
	 * The unique name for the player that connected.
	 */
	public String uniqueName;
	/**
	 * The index of the color of this player on the sprite sheet. See
	 * {@link MascotColor#getIndex()} for an explanation of this number.
	 *
	 * @see MascotColor#getIndex()
	 */
	public double colorIndex;
	/**
	 * What round the match is on at time of join.
	 */
	public double waveNum;
}
