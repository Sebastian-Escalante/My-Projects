package cruftyKrab.game.events;

import com.ikalagaming.event.Event;

import cruftyKrab.network.Connection;

/**
 * A new player connected to the server, and a socket has been initialized.
 *
 * @author Ches Burks
 *
 */
public class PlayerConnected extends Event {
	private Connection connection;

	/**
	 * Constructs a new event for the given connection.
	 *
	 * @param socket the connection to the player.
	 */
	public PlayerConnected(Connection socket) {
		this.connection = socket;
	}

	/**
	 * Returns the connection to the player.
	 *
	 * @return the managed socket connection to the player.
	 */
	public Connection getConnection() {
		return this.connection;
	}
}
