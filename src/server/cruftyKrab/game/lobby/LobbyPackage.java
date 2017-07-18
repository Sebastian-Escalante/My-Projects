package cruftyKrab.game.lobby;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.event.EventManager;
import com.ikalagaming.event.Listener;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.packages.Package;
import com.ikalagaming.packages.PackageManager;
import com.ikalagaming.util.DuplicateEntry;
import com.ikalagaming.util.IntegerTree;

import cruftyKrab.ai.pathing.Pathfinding;
import cruftyKrab.ai.pathing.PathingMap;
import cruftyKrab.game.events.Freeze;
import cruftyKrab.game.events.JoinMatch;
import cruftyKrab.game.events.SuddenDeath;
import cruftyKrab.network.Connection;
import cruftyKrab.network.WSServer;
import tiled.core.Map;
import tiled.io.TMXMapReader;

/**
 * Serves as a lobby for players and handles matches.
 *
 * @author Ches Burks
 *
 */
public class LobbyPackage implements Package {

	private static final double version = 1.0;
	/**
	 * The package name for the lobby. The name that is returned by
	 * {@link #getName()}. This is publicly readable to ease development.
	 */
	public static final String packageName = "Lobby";

	private Set<Listener> listeners;
	private ArrayList<Match> curMatches;
	private IntegerTree matchIDTree;
	private ReentrantLock idLock;

	/**
	 * Connections of players that are in the lobby.
	 */
	private ArrayList<Connection> inLobby;
	private ReentrantLock connectionLock;
	private ReentrantLock matchLock;
	private Pathfinding pathfinding;
	private Map map;
	private PathingMap pathingMap;

	/**
	 * Adds a player to the lobby. Returns a success value which is false if the
	 * lobby package is not enabled.
	 *
	 * @param player the connection to add to the lobby
	 * @return true on success, false if the lobby is disabled
	 */
	public boolean addToLobby(Connection player) {
		if (!PackageManager.getInstance().isEnabled(this)) {
			Logging.warning(LobbyPackage.packageName,
					"Attepmting to add players to lobby when disabled.");
			return false;
		}
		player.setManager(EventManager.getInstance());
		this.connectionLock.lock();
		try {
			this.inLobby.add(player);
		}
		finally {
			this.connectionLock.unlock();
		}
		Logging.finer(LobbyPackage.packageName, "New player added to lobby.");

		return true;
	}

	/**
	 * Adds the player to the first available match.
	 *
	 * @param player the connection to the player
	 */
	protected void addToMatch(Connection player) {

		int matches;
		int bestID;
		Match toAdd;

		this.idLock.lock();
		this.matchLock.lock();
		try {
			// join the first open game
			bestID = -1;
			matches = this.curMatches.size();
			for (int i = 0; i < matches; ++i) {
				toAdd = this.curMatches.get(i);
				if (toAdd.getPlayerCount() < Match.MAX_PLAYERS) {
					bestID = toAdd.getMatchID();
					break;
				}
			}
			if (bestID == -1) {// or create one if they are all full
				bestID = this.newMatch();
			}

			JoinMatch jm =
					new JoinMatch(this.getMatch(bestID).getMatchID(), player);
			EventManager.getInstance().fireEvent(jm);

		}
		finally {
			this.matchLock.unlock();
			this.idLock.unlock();
		}
	}

	/**
	 * Destroys the match defined by the supplied match ID.
	 *
	 * @param matchID the ID of the match to destroy
	 */
	protected void destoryMatch(final int matchID) {
		this.idLock.lock();
		boolean hasMatch;
		try {
			hasMatch = this.matchIDTree.contains(matchID);
			if (!hasMatch) {
				Logging.finer(LobbyPackage.packageName,
						"Removing unknown match " + matchID);
				return;
			}
			this.matchLock.lock();

			try {
				Match toRemove = null;
				for (Match m : this.curMatches) {
					if (m.getMatchID() == matchID) {
						toRemove = m;
						break;
					}
				}

				if (toRemove == null) {
					Logging.finer(LobbyPackage.packageName,
							"Could not find match " + matchID);
					return;
				}
				toRemove.kickAllToLobby();
				this.curMatches.remove(toRemove);
				this.matchIDTree.remove(matchID);
			}
			finally {
				this.matchLock.unlock();
			}
		}
		finally {
			this.idLock.unlock();
		}
	}

	/**
	 * Fire freeze events to all matches.
	 */
	protected void freeze() {
		this.matchLock.lock();
		try {
			for (Match m : this.curMatches) {
				m.getEventManager().fireEvent(new Freeze());
			}
		}
		finally {
			this.matchLock.unlock();
		}
	}

	@Override
	public Set<Listener> getListeners() {
		if (this.listeners == null) {
			this.listeners = new HashSet<>();
			this.listeners.add(new LobbyListener(this));
		}
		return this.listeners;
	}

	/**
	 * Gets a match based on the ID. The life span of the match returned by this
	 * should be minimized, as its not safe.
	 *
	 * @param matchID the ID of the match
	 * @return the match with that ID, or null if there isn't one
	 */
	protected Match getMatch(final int matchID) {
		this.idLock.lock();
		boolean hasMatch;
		try {
			hasMatch = this.matchIDTree.contains(matchID);
			if (!hasMatch) {
				Logging.finer(LobbyPackage.packageName,
						"Joining unknown match " + matchID);
				return null;
			}
			this.matchLock.lock();

			try {
				Match ret = null;
				for (Match m : this.curMatches) {
					if (m.getMatchID() == matchID) {
						ret = m;
						break;
					}
				}

				if (ret == null) {
					Logging.finer(LobbyPackage.packageName,
							"Could not find match " + matchID);
					return null;
				}
				return ret;
			}
			finally {
				this.matchLock.unlock();
			}
		}
		finally {
			this.idLock.unlock();
		}
	}

	@Override
	public String getName() {
		return LobbyPackage.packageName;
	}

	/**
	 * Returns the next available match ID.
	 *
	 * @return the next new match ID
	 */
	protected int getNewMatchID() {
		int id;
		this.idLock.lock();
		try {
			id = this.matchIDTree.getSmallestUnusedInt();
		}
		finally {
			this.idLock.unlock();
		}
		return id;
	}

	@Override
	public double getVersion() {
		return LobbyPackage.version;
	}

	/**
	 * Checks if a match exists with a supplied match ID.
	 *
	 * @param matchID the ID of the match
	 * @return true if it is registered, false if no match registered that ID
	 */
	protected boolean matchExists(final int matchID) {
		boolean exists;
		this.idLock.lock();
		try {
			exists = this.matchIDTree.contains(matchID);
		}
		finally {
			this.idLock.unlock();
		}
		return exists;
	}

	/**
	 * Create a new match with the given host.
	 *
	 * @return the ID of the newly created match
	 */
	protected int newMatch() {
		this.matchLock.lock();
		this.idLock.lock();
		int id = this.registerNewMatchID();
		Match match = new Match(id, this.pathfinding, this.pathingMap);

		try {
			this.curMatches.add(match);
		}
		finally {
			this.idLock.unlock();
			this.matchLock.unlock();
		}
		return id;
	}

	@Override
	public boolean onDisable() {
		return true;
	}

	@Override
	public boolean onEnable() {
		return true;
	}

	@Override
	public boolean onLoad() {

		PackageManager.getInstance().registerCommand("suddenDeath", this);
		PackageManager.getInstance().registerCommand("freeze", this);
		this.matchLock = new ReentrantLock();
		this.curMatches = new ArrayList<>();
		this.inLobby = new ArrayList<>();
		this.connectionLock = new ReentrantLock();
		this.idLock = new ReentrantLock();
		this.idLock.lock();
		try {
			this.matchIDTree = new IntegerTree();
		}
		finally {
			this.idLock.unlock();
		}

		Logging.finest(LobbyPackage.packageName, "Loading map...");
		TMXMapReader reader = new TMXMapReader();

		try {
			this.map = reader.readMap("map.tmx");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			Logging.warning(LobbyPackage.packageName, "Can't parse map.");
			return false;
		}

		this.pathingMap = new PathingMap(this.map);
		Logging.finest(LobbyPackage.packageName, "Done loading map!");
		this.pathfinding = new Pathfinding();
		Logging.finest(LobbyPackage.packageName,
				"Generating pathfinding map...");
		this.pathfinding.processMap(this.map);
		Logging.finest(LobbyPackage.packageName, "Done generating paths!");
		return true;
	}

	@Override
	public boolean onUnload() {
		PackageManager.getInstance().unregisterPackageCommands(this);
		if (!this.curMatches.isEmpty()) {
			this.curMatches.forEach(m -> m.shutdown());
			this.curMatches.clear();
		}
		if (!this.inLobby.isEmpty()) {
			this.inLobby.forEach(c -> c.close(WSServer.CLOSE_NORMAL));
			this.inLobby.clear();
		}
		this.curMatches = null;
		this.inLobby = null;
		return true;
	}

	/**
	 * Registers a new match ID, and returns it.
	 *
	 * @return the newly registered match ID
	 */
	protected int registerNewMatchID() {
		int id = -1;
		this.idLock.lock();
		try {
			id = this.matchIDTree.getSmallestUnusedInt();
			this.matchIDTree.insert(id);
		}
		catch (DuplicateEntry e) {
			e.printStackTrace(System.err);
		}
		finally {
			this.idLock.unlock();
		}
		return id;
	}

	/**
	 * Fire sudden death events to all matches.
	 */
	protected void suddenDeath() {
		this.matchLock.lock();
		try {
			for (Match m : this.curMatches) {
				m.getEventManager().fireEvent(new SuddenDeath());
			}
		}
		finally {
			this.matchLock.unlock();
		}
	}
}
