package cruftyKrab.network.messages;

import cruftyKrab.game.MascotColor;

/**
 * Enough to define a specific entity.
 *
 * @author Ches Burks
 *
 */
public class EntityInfo {
	/**
	 * The unique id of the entity.
	 */
	public String uniqueID;
	/**
	 * The name, or type of entity.
	 */
	public String name;
	/**
	 * The index of the color of this player on the sprite sheet. See
	 * {@link MascotColor#getIndex()} for an explanation of this number.
	 *
	 * @see MascotColor#getIndex()
	 */
	public double colorIndex;
}
