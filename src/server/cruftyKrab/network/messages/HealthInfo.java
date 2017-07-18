package cruftyKrab.network.messages;

/**
 * Health of an entity.
 *
 * @author Ches Burks
 *
 */
public class HealthInfo {
	/**
	 * Max health number.
	 */
	public double maxHealth;
	/**
	 * Current health. Should be >0, <max.
	 */
	public double curHealth;

}
