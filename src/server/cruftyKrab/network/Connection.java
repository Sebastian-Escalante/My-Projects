package cruftyKrab.network;

import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.locks.ReentrantLock;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;
import com.ikalagaming.event.EventManager;
import com.ikalagaming.logging.Logging;

/**
 * A connection that is currently made to a client.
 *
 * @author Ches Burks
 *
 */
public class Connection {
	private WebSocket connection;

	private EventManager manager;

	private ReentrantLock managerLock;

	private Gson gson;

	/**
	 * Constructs a connection to a client.
	 *
	 * @param socket the WebSocket to the client
	 */
	public Connection(WebSocket socket) {
		this.connection = socket;
		this.managerLock = new ReentrantLock();
		this.manager = EventManager.getInstance();
		this.gson = new Gson();
	}

	/**
	 * Closes the connection. Codes are defined in {@link WSServer}.
	 *
	 * @param code the closing code
	 */
	public void close(final int code) {
		if (this.connection == null) {
			return;
		}
		if (this.connection.isClosed()) {
			return;
		}
		this.connection.close(code);
	}

	/**
	 * Get the gson instance from this connection.
	 *
	 * @return the gson instance
	 */
	public Gson getGson() {
		return this.gson;
	}

	/**
	 * Returns the event manager handling events from this connection. Default
	 * to the static manager, but can be changed, for example to the manager for
	 * a match.
	 *
	 * @return The manager concerned with messages about the connection
	 */
	public EventManager getManager() {
		EventManager mgr;
		this.managerLock.lock();
		try {
			mgr = this.manager;
		}
		finally {
			this.managerLock.unlock();
		}
		return mgr;
	}

	/**
	 * Convert an object to JSON and send it to the client. If the object is a
	 * String it just sends it.
	 *
	 * @param toJsonify the object to jsonify and send
	 */
	public void sendMessage(final Object toJsonify) {
		if (toJsonify instanceof String) {
			this.sendMessage((String) toJsonify);
			return;
		}
		this.sendMessage(this.gson.toJson(toJsonify));
	}

	/**
	 * Sends a message to the WebSocket.
	 *
	 * @param message the string to send. Should be JSON.
	 */
	public void sendMessage(final String message) {
		try {
			this.connection.send(message);
		}
		catch (@SuppressWarnings("unused") NotYetConnectedException e) {
			Logging.warning("Connection",
					"Sending message to client that hasn't connected yet.");
		}
	}

	/**
	 * Sets the manager concerned with messages about this connection.
	 *
	 * @param newManager the new event manager
	 */
	public void setManager(EventManager newManager) {
		this.managerLock.lock();
		try {
			this.manager = newManager;
		}
		finally {
			this.managerLock.unlock();
		}
	}

}
