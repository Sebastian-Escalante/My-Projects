package cruftyKrab.network.messages.out;

import cruftyKrab.network.messages.EntityInfo;

/**
 * A player left the game
 *
 * @author Ches Burks
 *
 */
public class PlayerLeft {
	/**
	 * The type of event (Equal to this class name, so it can be parsed.)
	 */
	public String eventType = "PlayerLeft";
	/**
	 * The entity that left.
	 */
	public EntityInfo entity;
}
