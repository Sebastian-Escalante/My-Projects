package cruftyKrab.network.events;

import com.ikalagaming.event.Event;

import cruftyKrab.network.Connection;
import cruftyKrab.network.WSServer;

/**
 * A WebSocket connection was closed.
 *
 * @author Ches Burks
 *
 */
public class ConnectionClosed extends Event {

	private Connection connection;
	private final int code;

	/**
	 * A Connection was closed for some reason. Close codes are defined in
	 * {@link WSServer}.
	 *
	 * @param closed the connection that was closed
	 * @param closeCode the close code
	 */
	public ConnectionClosed(Connection closed, final int closeCode) {
		this.connection = closed;
		this.code = closeCode;
	}

	/**
	 * The close code. Close codes are defined in {@link WSServer}.
	 *
	 * @return the code
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * The closed connection
	 *
	 * @return the connection
	 */
	public Connection getConnection() {
		return this.connection;
	}

}
