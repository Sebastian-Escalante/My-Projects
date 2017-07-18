package cruftyKrab.network;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.ikalagaming.event.EventManager;
import com.ikalagaming.logging.Logging;

import cruftyKrab.game.events.PlayerConnected;
import cruftyKrab.network.events.ConnectionClosed;
import cruftyKrab.network.events.MessageReceived;

/**
 * A WebSocket server using Nathan Rajlich's java_websocket library.
 *
 * @author Ches Burks
 *
 */
public class WSServer extends WebSocketServer {

	/**
	 * The close code for Normal closure; the connection successfully completed
	 * whatever purpose for which it was created.
	 */
	public static final int CLOSE_NORMAL = 1000;
	/**
	 * The endpoint is going away, either because of a server failure or because
	 * the browser is navigating away from the page that opened the connection.
	 */
	public static final int CLOSE_GOING_AWAY = 1001;
	/**
	 * The endpoint is terminating the connection due to a protocol error.
	 */
	public static final int CLOSE_PROTOCOL_ERROR = 1002;
	/**
	 * The connection is being terminated because the endpoint received data of
	 * a type it cannot accept (for example, a text-only endpoint received
	 * binary data).
	 */
	public static final int CLOSE_UNSUPPORTED = 1003;
	/**
	 * The endpoint is terminating the connection because a message was received
	 * that contained inconsistent data (e.g., non-UTF-8 data within a text
	 * message).
	 */
	public static final int UNSUPPORTED_DATA = 1007;
	/**
	 * The endpoint is terminating the connection because it received a message
	 * that violates its policy. This is a generic status code, used when codes
	 * {@link #CLOSE_UNSUPPORTED 1003} and {@link #CLOSE_TOO_LARGE 1009} are not
	 * suitable.
	 */
	public static final int POLICY_VIOLATION = 1008;
	/**
	 * The endpoint is terminating the connection because a data frame was
	 * received that is too large.
	 */
	public static final int CLOSE_TOO_LARGE = 1009;
	/**
	 * The client is terminating the connection because it expected the server
	 * to negotiate one or more extension, but the server didn't.
	 */
	public static final int MISSING_EXTENSION = 1010;
	/**
	 * The server is terminating the connection because it encountered an
	 * unexpected condition that prevented it from fulfilling the request.
	 */
	public static final int INTERNAL_ERROR = 1011;
	/**
	 * The server is terminating the connection because it is restarting.
	 */
	public static final int SERVICE_RESTART = 1012;
	/**
	 * The server is terminating the connection due to a temporary condition,
	 * e.g. it is overloaded and is casting off some of its clients.
	 */
	public static final int TRY_AGAIN_LATER = 1013;

	private static int counter = 0;
	private static final String LOG_NAME = "Web Socket";

	private HashMap<WebSocket, Connection> connectionMap;

	/**
	 * Creates a new web socket server given the port and {@link Draft} The
	 * draft defines things in a websocket specification which are not common
	 * such as the way the handshake is read or frames are transfered.
	 *
	 * @param address the address and port to be listening on
	 * @param d the draft to use
	 */
	public WSServer(InetSocketAddress address, Draft d) {
		super(address, Collections.singletonList(d));
		this.connectionMap = new HashMap<>();
	}

	/**
	 * Creates a new web socket server given the port and {@link Draft} The
	 * draft defines things in a websocket specification which are not common
	 * such as the way the handshake is read or frames are transfered.
	 *
	 * @param port the port number to listen on
	 * @param d the draft to use
	 * @throws UnknownHostException when defined by the library, unknown to
	 *             implementor (Ches)
	 */
	public WSServer(int port, Draft d) throws UnknownHostException {
		super(new InetSocketAddress(port), Collections.singletonList(d));
		this.connectionMap = new HashMap<>();
	}

	/**
	 * Closes all the open connections with a {@link #CLOSE_NORMAL normal} close
	 * code.
	 */
	public void closeAll() {
		ArrayList<Connection> connections = new ArrayList<>();
		connections.addAll(this.connectionMap.values());
		for (Connection c : connections) {
			c.close(WSServer.CLOSE_NORMAL);
		}
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason,
			boolean remote) {
		WSServer.counter--;
		Logging.info(WSServer.LOG_NAME, "Closed connection. Now "
				+ WSServer.counter + " clients connected.");

		Connection c = this.connectionMap.get(conn);
		this.connectionMap.remove(conn);
		if (c == null) {
			Logging.warning(WSServer.LOG_NAME,
					"Closed a nonexistent connection");
			return;
		}
		ConnectionClosed event = new ConnectionClosed(c, code);
		c.getManager().fireEvent(event);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		Logging.warning(WSServer.LOG_NAME,
				"Error occurred (" + ex.getLocalizedMessage() + ")");
		ex.printStackTrace(System.err);
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer blob) {
		// conn.send(blob); // echo
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		// conn.send(message);
		// Logging.finer(WSServer.LOG_NAME, "Got message '" + message + "'");
		Connection connection = this.connectionMap.get(conn);
		if (connection == null) {
			Logging.warning(WSServer.LOG_NAME,
					"Received message from unmapped connection");
			return;
		}
		// alert other modules of message
		MessageReceived event = new MessageReceived(message, connection);
		EventManager.getInstance().fireEvent(event);
		connection.getManager().fireEvent(event);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		WSServer.counter++;
		Logging.fine(WSServer.LOG_NAME,
				"Opened connection number " + WSServer.counter);
		Connection c = new Connection(conn);
		// store a reference so that you can find connections on message
		this.connectionMap.put(conn, c);
		// alert other modules of new connection
		PlayerConnected event = new PlayerConnected(c);
		c.getManager().fireEvent(event);
	}

	/**
	 * Does processing for when a fragment (frame) of a message is transmitted.
	 *
	 * @param conn the connection sending data
	 * @param frame the data sent in that frame
	 */
	public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
		FrameBuilder builder = (FrameBuilder) frame;
		builder.setTransferemasked(false);
		conn.sendFrame(frame);
	}

}
