package cruftyKrab.network.messages.out;

import cruftyKrab.network.messages.EntityInfo;
import cruftyKrab.network.messages.Vect;

/**
 * An entity was spawned.
 *
 * @author Ches Burks
 *
 */
public class EntitySpawned {
	/**
	 * The type of event (Equal to this class name, so it can be parsed.)
	 */
	public String eventType = "EntitySpawned";
	/**
	 * The entity that was spawned.
	 */
	public EntityInfo entity;
	/**
	 * Where the entity spawned.
	 */
	public Vect position;
}
