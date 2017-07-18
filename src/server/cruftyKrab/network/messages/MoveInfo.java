package cruftyKrab.network.messages;

/**
 * Some change in position or velocity that occurred.
 *
 * @author Ches Burks
 *
 */
public class MoveInfo {
	/**
	 * The entity that moved.
	 */
	public EntityInfo entity;
	/**
	 * The new position of that entity.
	 */
	public Vect position;
	/**
	 * The way the player is facing
	 */
	public String facing;
}
