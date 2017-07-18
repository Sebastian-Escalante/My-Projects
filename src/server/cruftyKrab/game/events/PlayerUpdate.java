package cruftyKrab.game.events;

import cruftyKrab.network.Connection;

/**
 * A player is attempting to join a match.
 *
 * @author Ches Burks
 *
 */
public class PlayerUpdate extends CancellableEvent {

	private final Connection sender;
	private final double x;
	private final double y;
	private final String facing;

	/**
	 * Creates a {@link PlayerUpdate} event.
	 *
	 * @param xPos x position
	 * @param yPos y position
	 * @param facingS The direction they are facing
	 * @param connection the connection that requested the join
	 */
	public PlayerUpdate(final double xPos, final double yPos,
			final String facingS, final Connection connection) {
		this.x = xPos;
		this.y = yPos;
		this.facing = facingS;
		this.sender = connection;
	}

	/**
	 * Returns the new way the player is facing.
	 *
	 * @return the y position
	 */
	public String getFacing() {
		return this.facing;
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
	 * Returns the new X position.
	 *
	 * @return the x position
	 */
	public double getX() {
		return this.x;
	}

	/**
	 * Returns the new Y position.
	 *
	 * @return the y position
	 */
	public double getY() {
		return this.y;
	}
}
