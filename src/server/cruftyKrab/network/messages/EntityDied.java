package cruftyKrab.network.messages;

/**
 * An entity died.
 *
 * @author Ches Burks
 *
 */
public class EntityDied {
	/**
	 * The type of event (Equal to this class name, so it can be parsed.)
	 */
	public String eventType = "EntityDied";
	/**
	 * The unique id of the entity.
	 */
	public String uniqueID;
	/**
	 * How many points the player currently has, after the entity died.
	 */
	public double points;
}
