package cruftyKrab.network.messages;

/**
 * A score mapping for players.
 *
 * @author Ches Burks
 *
 */
public class ScoreInfo {
	/**
	 * The player.
	 */
	public EntityInfo entity;
	/**
	 * The score of that entity.
	 */
	public double score;

	/**
	 * The name of the color of the mascot.
	 */
	public String colorName;
}
