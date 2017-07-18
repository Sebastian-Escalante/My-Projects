package cruftyKrab.network.events;

import com.ikalagaming.event.Event;

import cruftyKrab.network.Connection;

/**
 * A message was received by a connection. This should be JSON.
 *
 * @author Ches Burks
 *
 */
public class MessageReceived extends Event {

	private final String message;
	private Connection socket;

	/**
	 * A message was received by a connection.
	 *
	 * @param msg the message received.
	 * @param on the connection that sent the message
	 */
	public MessageReceived(final String msg, Connection on) {
		this.message = msg;
		this.socket = on;
	}

	/**
	 * Returns the message sent by the client. This should be JSON.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Returns the connection that sent the message
	 *
	 * @return the connection
	 */
	public Connection getSocket() {
		return this.socket;
	}
}
