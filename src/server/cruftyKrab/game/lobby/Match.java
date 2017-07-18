package cruftyKrab.game.lobby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.event.EventManager;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.packages.Package;
import com.ikalagaming.packages.PackageManager;

import cruftyKrab.ai.pathing.Pathfinding;
import cruftyKrab.ai.pathing.PathingMap;
import cruftyKrab.network.Connection;

/**
 * A game that is currently going on, or at least waiting to be started. It can
 * handle multiple players, and multiple matches may be going on at any given
 * time. In general, the first player creates a new match, and subsequent
 * players can either join an existing match or start their own.
 *
 * @author Ches Burks
 *
 */
public class Match {
	/**
	 * The maximum number of players that can be in a match at once.
	 */
	public static final int MAX_PLAYERS = 10;
	/**
	 * The name of whoever created the match.
	 */
	private String host;
	private ReentrantLock hostLock;
	/**
	 * Connections to clients that are in the match.
	 */
	private ArrayList<Connection> connections;
	/**
	 * Maps unique names to owning connection.
	 */
	private HashMap<String, Connection> playerMap;
	private ReentrantLock connectionLock;
	/**
	 * The event manager for match specific events. This is used so that events
	 * from one game do not crowd events from another.
	 */
	private EventManager eventMgr;

	private MatchDirector director;
	private UpdateThread updater;

	private final int matchID;

	/**
	 * The reference to the pathfinding instance
	 */
	protected Pathfinding pathfinding;
	/**
	 * The reference to the pathing map
	 */
	protected PathingMap map;

	/**
	 * Creates a new match for players, with a dedicated event manager.
	 *
	 * @param id The unique match ID for this match.
	 * @param pf The pathfinding instance to use
	 * @param pMap the pathing map to use
	 *
	 * @see Match#shutdown()
	 */
	public Match(final int id, Pathfinding pf, PathingMap pMap) {
		this.connections = new ArrayList<>();
		this.playerMap = new HashMap<>();
		this.eventMgr = new EventManager();
		this.hostLock = new ReentrantLock();
		this.connectionLock = new ReentrantLock();
		this.matchID = id;
		this.director = new MatchDirector(this);
		this.eventMgr.registerEventListeners(this.director);
		this.updater = new UpdateThread(this.eventMgr);
		this.updater.start();
		this.pathfinding = pf;
		this.map = pMap;
	}

	/**
	 * Send an object as json to all connections
	 *
	 * @param jsonable an object that can be turned into json
	 */
	protected void broadcast(Object jsonable) {
		this.connectionLock.lock();
		try {
			/*
			 * Logging.finest("Match", "Dispatching to " +
			 * this.connections.size() + " connections.");
			 */
			for (Connection c : this.connections) {
				c.sendMessage(jsonable);
			}
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Returns the connection mapped to the given unique name.
	 *
	 * @param playerName the unique (entity) name of the player
	 * @return the connection controlling that player, or null if none exists
	 */
	protected Connection getConnection(final String playerName) {
		if (playerName == null) {
			return null;
		}
		Connection c;
		this.connectionLock.lock();
		try {
			c = this.playerMap.get(playerName);
		}
		finally {
			this.connectionLock.unlock();
		}
		return c;
	}

	/**
	 * Returns the event manager for this match.
	 *
	 * @return the dedicated event manager
	 */
	protected EventManager getEventManager() {
		return this.eventMgr;
	}

	/**
	 * Returns the name of the host. The host is the player that created the
	 * match, and hence the first to join. If there is no host, an empty string
	 * is returned.
	 *
	 * @return the username of the host
	 */
	public String getHostName() {
		String hostName;
		this.hostLock.lock();
		try {
			hostName = this.host == null ? "" : this.host;
		}
		finally {
			this.hostLock.unlock();
		}
		return hostName;
	}

	/**
	 * Returns the unique Match ID for this match.
	 *
	 * @return the integer id of this match.
	 */
	public int getMatchID() {
		return this.matchID;
	}

	/**
	 * Returns the name the connection is mapped to.
	 *
	 * @param connection the connection controlling that player, or null if none
	 *            exists
	 * @return the unique (entity) name of the player
	 */
	protected String getName(final Connection connection) {
		if (connection == null) {
			return null;
		}
		String name = "";
		this.connectionLock.lock();
		try {
			for (String s : this.playerMap.keySet()) {
				if (this.playerMap.get(s).equals(connection)) {
					name = s;
					break;
				}
			}
		}
		finally {
			this.connectionLock.unlock();
		}
		return name;
	}

	/**
	 * Returns the number of players in the match.
	 *
	 * @return how many connections are owned by this match
	 */
	public int getPlayerCount() {
		this.connectionLock.lock();
		try {
			return this.connections.size();
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Kicks all the players in the match back out to the lobby.
	 */
	public void kickAllToLobby() {
		if (this.connections.isEmpty()) {
			return;
		}
		Logging.finer(LobbyPackage.packageName, "Kicking all players to lobby");
		if (PackageManager.getInstance().isLoaded(LobbyPackage.packageName)) {
			Package pack = PackageManager.getInstance()
					.getPackage(LobbyPackage.packageName);
			if (pack != null) {
				LobbyPackage lobby = (LobbyPackage) pack;
				this.connectionLock.lock();
				try {
					// TODO alert players of this
					this.connections.forEach(c -> lobby.addToLobby(c));
					this.playerMap.clear();
					this.connections.clear();
				}
				finally {
					this.connectionLock.unlock();
				}
			}
		}
	}

	/**
	 * Map a unique player name to the connection
	 *
	 * @param name the name of the player's mascot
	 * @param c the connection controlling that mascot
	 */
	protected void mapPlayer(final String name, Connection c) {
		this.connectionLock.lock();
		try {
			this.connections.add(c);
			this.playerMap.put(name, c);
		}
		finally {
			this.connectionLock.unlock();
		}
	}

	/**
	 * Removes the connection and mapping to it.
	 *
	 * @param c the connection to remove.
	 * @return the unique name of the player that left, or null if not found
	 */
	protected String removeConnection(Connection c) {
		this.connectionLock.lock();
		try {
			this.connections.remove(c);
			String name = null;
			for (String s : this.playerMap.keySet()) {
				if (this.playerMap.get(s).equals(c)) {
					name = s;
					break;
				}
			}
			if (name != null) {
				this.playerMap.remove(name);
				return name;
			}
		}
		finally {
			this.connectionLock.unlock();
			// reset the connection's event manager
			c.setManager(EventManager.getInstance());
		}
		return null;
	}

	/**
	 * Kicks all players out to the lobby and cleans up. This also shuts down
	 * the event manager, hence is important to call before dereferencing.
	 */
	public void shutdown() {
		this.updater.terminate();
		this.director.shutdown();
		this.eventMgr.unregisterEventListeners(this.director);
		this.kickAllToLobby();
		this.eventMgr.shutdown();
		this.pathfinding = null;
		this.map = null;
	}

}
