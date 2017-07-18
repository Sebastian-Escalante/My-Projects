package cruftyKrab.game.events;

import cruftyKrab.network.Connection;

/**
 * An entity has died.
 *
 * @author Ches Burks
 *
 */
public class EntityDiedEvent extends CancellableEvent {

	private final Connection sender;
	private final String id;
	private final double pts;

	/**
	 * Creates a {@link EntityDiedEvent} event.
	 *
	 * @param uniqueID the unique ID of the entity that died
	 * @param connection the connection that requested the join
	 * @param points the number of points the player at the other end of the
	 *            connection has
	 */
	public EntityDiedEvent(final String uniqueID, final Connection connection,
			final double points) {
		this.id = uniqueID;
		this.sender = connection;
		this.pts = points;
	}

	/**
	 * Returns the connection that requested the join
	 *
	 * @return the connection
	 */
	public Connection getSender() {
		return this.sender;
	}

	/**
	 * Returns the id of the dead entity.
	 *
	 * @return the unique ID
	 */
	public String getUniqueID() {
		return this.id;
	}

	/**
	 * Returns how many points the player sending the event now has.
	 * 
	 * @return the current points for the player that owns the connection
	 */
	public double getPoints() {
		return this.pts;
	}
}
