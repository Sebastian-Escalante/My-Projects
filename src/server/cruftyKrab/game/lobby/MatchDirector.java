package cruftyKrab.game.lobby;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.ikalagaming.entity.component.Health;
import com.ikalagaming.event.EventHandler;
import com.ikalagaming.event.EventManager;
import com.ikalagaming.event.Listener;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.util.DuplicateEntry;
import com.ikalagaming.util.IntegerTree;

import cruftyKrab.game.Location;
import cruftyKrab.game.Mascot;
import cruftyKrab.game.MascotColor;
import cruftyKrab.game.events.EntityDiedEvent;
import cruftyKrab.game.events.Freeze;
import cruftyKrab.game.events.JoinMatch;
import cruftyKrab.game.events.MatchOver;
import cruftyKrab.game.events.PlayerUpdate;
import cruftyKrab.game.events.RoundOver;
import cruftyKrab.game.events.SuddenDeath;
import cruftyKrab.game.events.Update;
import cruftyKrab.network.events.ConnectionClosed;
import cruftyKrab.network.messages.EntityDied;
import cruftyKrab.network.messages.EntityInfo;
import cruftyKrab.network.messages.MoveInfo;
import cruftyKrab.network.messages.ScoreInfo;
import cruftyKrab.network.messages.Vect;
import cruftyKrab.network.messages.out.EntitySpawned;
import cruftyKrab.network.messages.out.GameOver;
import cruftyKrab.network.messages.out.MoveSet;
import cruftyKrab.network.messages.out.PlayerLeft;
import cruftyKrab.network.messages.out.Welcome;

/**
 * The brain behind a match. Handles AI, spawning, etc. Named after The Director
 * from Left 4 Dead. Should listen to match events.
 *
 * @author Ches Burks
 *
 */
public class MatchDirector implements Listener {
	private final static int MIN_MAP_X = 1;
	private final static int MIN_MAP_Y = 1;
	private final static int MAX_MAP_X = 30;
	private final static int MAX_MAP_Y = 30;
	private final static int MAP_WIDTH =
			MatchDirector.MAX_MAP_X - MatchDirector.MIN_MAP_X;
	private final static int MAP_HEIGHT =
			MatchDirector.MAX_MAP_Y - MatchDirector.MIN_MAP_Y;

	private static int maxAI(final int round) {
		if (round <= 0) {
			return 0;
		}
		return 3 * round;
	}

	private HashSet<Mascot> ai;
	private HashSet<Mascot> players;

	private ReentrantLock aiLock;
	private ReentrantLock playerLock;

	private IntegerTree colorTree;
	private Match parent;

	private int round;
	private int spawnedThisRound;

	private Map<Mascot, Mascot> targets;
	// private Map<Mascot, Path> paths;

	private Map<Mascot, String> facing;

	private SecureRandom rng;

	private Map<Mascot, Double> points;

	private boolean suddenDeath;
	private boolean frozen;

	/**
	 * Creates a new director listener
	 *
	 * @param par the parent match
	 */
	public MatchDirector(Match par) {
		this.parent = par;
		this.ai = new HashSet<>();
		this.players = new HashSet<>();
		this.aiLock = new ReentrantLock();
		this.playerLock = new ReentrantLock();
		this.round = 1;
		this.colorTree = new IntegerTree();
		this.spawnedThisRound = 0;
		this.rng = new SecureRandom();

		this.targets =
				Collections.synchronizedMap(new HashMap<Mascot, Mascot>());
		// this.paths = Collections.synchronizedMap(new HashMap<Mascot,
		// Path>());
		this.facing =
				Collections.synchronizedMap(new HashMap<Mascot, String>());
		this.points =
				Collections.synchronizedMap(new HashMap<Mascot, Double>());
	}

	private void findNewTarget(Mascot enemy) {
		float lowest = Float.MAX_VALUE;
		Mascot lMasc = null;
		for (Mascot play : this.players) {
			Location l = (Location) play.getComponent(Location.TYPE_NAME);

			float dist = Location.getDistance(
					(Location) enemy.getComponent(Location.TYPE_NAME), l);
			if (dist < lowest) {
				lowest = dist;
				lMasc = play;
			}
		}
		if (lMasc == null) {
			Logging.finer("Match Director", "No players found!");
		}
		else {
			this.targets.put(enemy, lMasc);
		}
	}

	private void moveEnemies(long dt) {
		// 3.0f / seconds
		final float SPEED = 2000.0f / dt;
		this.aiLock.lock();
		this.playerLock.lock();
		try {
			for (Mascot m : this.ai) {
				Location loc = (Location) m.getComponent(Location.TYPE_NAME);
				// random movement.
				if (loc == null) {
					Logging.warning("Match Director",
							"Null location component.");
					break;
				}
				this.findNewTarget(m);
				Mascot target = this.targets.get(m);
				if (target == null) {
					target = this.targets.get(m);
					Logging.finer("Match Director",
							m.getName() + " is targeting " + target.getName());
				}
				Location tLoc =
						(Location) target.getComponent(Location.TYPE_NAME);
				// random movement.
				if (tLoc == null) {
					Logging.warning("Match Director",
							"Null target location component.");
					break;
				}

				// Point cur = new Point(Math.round(loc.getX()),
				// Math.round(loc.getY()));
				// Path toFollow = this.parent.pathfinding.getPath(cur, new
				// Point(
				// Math.round(tLoc.getX()), Math.round(tLoc.getY())));
				// this.parent.pathfinding.smoothPath(toFollow);
				// this.paths.put(m, toFollow);

				// System.out.println(toFollow.getTiles());
				// if (toFollow.steps() < 2) {
				// Logging.warning("Match Director",
				// "Path from (" + loc.getX() + ", " + loc.getY()
				// + ") to (" + tLoc.getX() + ", "
				// + tLoc.getY() + ") is too short ("
				// + toFollow.steps() + ").");
				// this.findNewTarget(m);
				// continue;
				// }

				// Point nextStep = toFollow.getTiles().get(1);

				Location next = new Location();

				int tries = 0;

				double angleRad;
				float dx, dy;
				angleRad = (Math.PI / 2) - Location.findAngleRads(loc, tLoc);

				dx = (float) (SPEED * Math.cos(angleRad));
				dy = (float) (SPEED * Math.sin(angleRad));

				while (!this.parent.map.isWalkable(Math.round(next.getX() + dx),
						(Math.round(next.getY() + dy)))) {
					++tries;
					if (tries > 1000) {
						break;
					}
					next.setX(this.rng.nextFloat() * MatchDirector.MAP_WIDTH
							+ MatchDirector.MIN_MAP_X);
					next.setY(this.rng.nextFloat() * MatchDirector.MAP_HEIGHT
							+ MatchDirector.MIN_MAP_Y);
					angleRad = (Math.PI / 2) - Location.findAngle(loc, next);
					while (angleRad < 0) {
						angleRad += Math.PI * 2;
					}
					dx = (float) (SPEED * Math.cos(angleRad));
					dy = (float) (SPEED * Math.sin(angleRad));
				}

				loc.setX(loc.getX() + dx);
				loc.setY(loc.getY() + dy);

				if (this.frozen) {
					loc.setX(1);
					loc.setY(1);
				}

				if (loc.getX() < MatchDirector.MIN_MAP_X) {
					loc.setX(MatchDirector.MIN_MAP_X);
				}
				if (loc.getY() < MatchDirector.MIN_MAP_Y) {
					loc.setY(MatchDirector.MIN_MAP_Y);
				}
				if (loc.getX() > MatchDirector.MAX_MAP_X) {
					loc.setX(MatchDirector.MAX_MAP_X);
				}
				if (loc.getY() > MatchDirector.MAX_MAP_Y) {
					loc.setY(MatchDirector.MAX_MAP_Y);
				}
			}

		}
		finally {
			this.playerLock.unlock();
			this.aiLock.unlock();
		}
	}

	/**
	 * Remove the connection and alert other clients of the loss of a player
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onConnectionClosed(ConnectionClosed event) {
		String uniqueName;
		uniqueName = this.parent.removeConnection(event.getConnection());
		if (uniqueName == null) {
			return;
		}
		Mascot left = null;
		this.playerLock.lock();
		try {
			for (Mascot m : this.players) {
				if (m.getName().equals(uniqueName)) {
					left = m;
					break;
				}
			}
			if (left != null) {
				this.players.remove(left);
				this.points.remove(left);
				this.colorTree.remove(left.getColor());
				left.destroy();
			}
			if (this.players.stream().filter((
					m) -> !((Health) m.getComponent(Health.TYPE_NAME)).isDead())
					.collect(Collectors.toSet()).isEmpty()) {
				Logging.finer("Match Director", "Game over!");
				GameOver go = new GameOver();
				ArrayList<ScoreInfo> scoreList = new ArrayList<>();
				for (Mascot m : this.points.keySet()) {
					ScoreInfo scoreInfo = new ScoreInfo();
					scoreInfo.entity = new EntityInfo();
					scoreInfo.entity.colorIndex = m.getColor();
					scoreInfo.entity.name = m.getUsername();
					scoreInfo.entity.uniqueID = m.getName();
					scoreInfo.score = this.points.get(m);
					MascotColor mColor = MascotColor.MAROON;
					for (MascotColor mc : MascotColor.values()) {
						if (mc.getIndex() == m.getColor()) {
							mColor = mc;
							break;
						}
					}
					scoreInfo.colorName = mColor.name();
					scoreList.add(scoreInfo);
				}
				scoreList.sort((a, b) -> Math.round((float) b.score)
						- Math.round((float) a.score));

				go.numScores = scoreList.size();
				go.scores = new ScoreInfo[scoreList.size()];
				go.scores = scoreList.toArray(go.scores);
				this.parent.broadcast(go);
				EventManager.getInstance()
						.fireEvent(new MatchOver(this.parent.getMatchID()));
			}
		}
		finally {
			this.playerLock.unlock();
		}
		if (left == null) {
			return;
		}

		this.parent.removeConnection(event.getConnection());

		EntityInfo info = new EntityInfo();
		info.name = left.getUsername();
		info.uniqueID = left.getName();
		PlayerLeft msg = new PlayerLeft();
		msg.entity = info;

		this.parent.broadcast(msg);
	}

	/**
	 * Kill and remove an entity, alert clients of the death.
	 *
	 * @param event the event.
	 */
	@EventHandler
	public void onEntityDied(EntityDiedEvent event) {
		Logging.finer("Match Director", event.getUniqueID() + " died.");
		this.playerLock.lock();
		try {
			ArrayList<Mascot> toRemove = new ArrayList<>();
			for (Mascot m : this.targets.keySet()) {
				if (this.targets.get(m).getName().equals(event.getUniqueID())) {
					toRemove.add(m);
				}
			}
			for (Mascot m : this.players) {
				if (this.parent.getName(event.getSender())
						.equals(m.getName())) {
					this.points.put(m, event.getPoints());
				}
			}
			for (Mascot m : toRemove) {
				this.targets.remove(m);
				this.findNewTarget(m);
			}

			for (Mascot m : this.players) {
				if (m.getName().equals(event.getUniqueID())) {
					Health h = ((Health) m.getComponent(Health.TYPE_NAME));
					h.setHealth(h.getMinHealth());
				}
			}

			// no players alive
			if (this.players.stream().filter((
					m) -> !((Health) m.getComponent(Health.TYPE_NAME)).isDead())
					.collect(Collectors.toSet()).isEmpty()) {
				Logging.finer("Match Director", "Game over!");
				GameOver go = new GameOver();
				ArrayList<ScoreInfo> scoreList = new ArrayList<>();
				for (Mascot m : this.points.keySet()) {
					ScoreInfo scoreInfo = new ScoreInfo();
					scoreInfo.entity = new EntityInfo();
					scoreInfo.entity.colorIndex = m.getColor();
					scoreInfo.entity.name = m.getUsername();
					scoreInfo.entity.uniqueID = m.getName();
					scoreInfo.score = this.points.get(m);
					MascotColor mColor = MascotColor.MAROON;
					for (MascotColor mc : MascotColor.values()) {
						if (mc.getIndex() == m.getColor()) {
							mColor = mc;
							break;
						}
					}
					scoreInfo.colorName = mColor.name();
					scoreList.add(scoreInfo);
				}
				scoreList.sort((a, b) -> Math.round((float) b.score)
						- Math.round((float) a.score));

				go.numScores = scoreList.size();
				go.scores = new ScoreInfo[scoreList.size()];
				go.scores = scoreList.toArray(go.scores);
				this.parent.broadcast(go);
				EventManager.getInstance()
						.fireEvent(new MatchOver(this.parent.getMatchID()));
				return;
			}
		}
		finally {
			this.playerLock.unlock();
		}

		this.aiLock.lock();
		try {
			for (Iterator<Mascot> it = this.ai.iterator(); it.hasNext();) {
				Mascot m = it.next();
				if (m.getName().equals(event.getUniqueID())) {
					it.remove();
					continue;
				}
			}
			Mascot player = null;
			final String name = this.parent.getName(event.getSender());
			for (Mascot m : this.players) {
				if (m.getName().equals(name)) {
					player = m;
					break;
				}
			}
			if (player == null) {
				Logging.warning("Match Director",
						"No player found to update points");
			}
			else {
				this.points.put(player, event.getPoints());
			}
			EntityDied ed = new EntityDied();
			ed.uniqueID = event.getUniqueID();

			this.parent.broadcast(ed);

			if (this.ai.isEmpty()) {
				this.parent.getEventManager().fireEvent(new RoundOver());
			}
		}
		finally {
			this.aiLock.unlock();
		}
	}

	/**
	 * Toggles freeze.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onFreeze(Freeze event) {
		this.frozen = !this.frozen;
	}

	/**
	 * Creates a new player based on the username of the player joining.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onJoinMatch(JoinMatch event) {
		if (event.isCancelled()) {
			return;
		}
		Mascot player = new Mascot("Player");
		player.setUsername("Player");
		this.playerLock.lock();
		try {
			this.players.add(player);
			int color = this.colorTree.getSmallestUnusedInt();
			if (color >= 0 && color < 10) {
				try {
					this.colorTree.insert(color);
				}
				catch (DuplicateEntry e) {
					e.printStackTrace(System.err);
				}
				player.setColor(color);
			}
			else {
				Logging.warning("Match Director",
						"Invalid color being created");
			}
		}
		finally {
			this.playerLock.unlock();
		}
		Location loc = (Location) player.getComponent(Location.TYPE_NAME);
		if (loc == null) {
			Logging.finer("Match Director",
					"Player doesn't have a location component");
		}
		else {
			loc.setX(21);
			loc.setY(22);
		}

		Welcome wel = new Welcome();
		wel.colorIndex = player.getColor();
		wel.uniqueName = player.getName();
		wel.waveNum = this.round;

		event.getSender().sendMessage(wel);

		EntitySpawned es = new EntitySpawned();
		EntityInfo info = new EntityInfo();
		info.name = player.getUsername();
		info.uniqueID = player.getName();
		info.colorIndex = player.getColor();
		es.entity = info;

		Vect pos = new Vect();
		if (loc != null) {
			pos.x = Math.round(loc.getX());
			pos.y = Math.round(loc.getY());
		}
		else {
			pos.x = 21;
			pos.y = 22;
		}
		es.position = pos;
		this.parent.broadcast(es);

		// spawn previous players
		for (Mascot m : this.players) {
			EntitySpawned es2 = new EntitySpawned();
			EntityInfo info2 = new EntityInfo();
			info2.name = m.getUsername();
			info2.uniqueID = m.getName();
			info2.colorIndex = m.getColor();
			es2.entity = info2;

			Location loc2 = (Location) m.getComponent(Location.TYPE_NAME);

			Vect pos2 = new Vect();
			if (loc2 != null) {
				pos2.x = Math.round(loc2.getX());
				pos2.y = Math.round(loc2.getY());
			}
			else {
				pos2.x = 21;
				pos2.y = 22;
			}
			es2.position = pos2;
			event.getSender().sendMessage(es2);
		}

		// spawn previous players
		for (Mascot m : this.ai) {
			EntitySpawned es2 = new EntitySpawned();
			EntityInfo info2 = new EntityInfo();
			info2.name = m.getUsername();
			info2.uniqueID = m.getName();
			info2.colorIndex = m.getColor();
			es2.entity = info2;

			Location loc2 = (Location) m.getComponent(Location.TYPE_NAME);

			Vect pos2 = new Vect();
			if (loc2 != null) {
				pos2.x = Math.round(loc2.getX());
				pos2.y = Math.round(loc2.getY());
			}
			else {
				pos2.x = 21;
				pos2.y = 22;
			}
			es2.position = pos2;
			event.getSender().sendMessage(es2);
		}

		this.parent.mapPlayer(player.getName(), event.getSender());
		// make the connection listen interact with this match
		event.getSender().setManager(this.parent.getEventManager());

	}

	/**
	 * Updates the player position
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onPlayerUpdate(PlayerUpdate event) {
		this.playerLock.lock();

		try {
			Mascot player = null;
			String name = this.parent.getName(event.getSender());
			for (Mascot m : this.players) {
				if (m.getName().equals(name)) {
					player = m;
					break;
				}
			}
			if (player == null) {
				Logging.finer("Match Director",
						"Trying to update non-existant player");
				return;
			}

			Location loc = (Location) player.getComponent(Location.TYPE_NAME);
			// 64 is the size of a tile
			loc.setX((float) event.getX() / 64);
			loc.setY((float) event.getY() / 64);

			this.facing.put(player, event.getFacing());
		}
		catch (Exception e) {
			Logging.warning("Match Director",
					"Error " + e.getMessage() + " in player update.");
		}
		finally {
			this.playerLock.unlock();
		}
	}

	/**
	 * Reset the round.
	 *
	 * @param roundOver the event
	 */
	@EventHandler
	public void onRoundOver(RoundOver roundOver) {
		Logging.finer("Match Director", "Round " + this.round + " over!");
		this.round++;
		this.spawnedThisRound = 0;
		this.playerLock.lock();
		try {
			for (Mascot m : this.players) {
				Health health = (Health) m.getComponent(Health.TYPE_NAME);
				health.setHealth(health.getMaxHealth());
			}
		}
		finally {
			this.playerLock.unlock();
		}
		cruftyKrab.network.messages.out.RoundOver ro =
				new cruftyKrab.network.messages.out.RoundOver();
		ro.roundNumber = this.round - 1;
		this.parent.broadcast(ro);
	}

	/**
	 * Toggles sudden death.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onSuddenDeath(SuddenDeath event) {
		this.suddenDeath = !this.suddenDeath;
	}

	/**
	 * Update the match
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onUpdate(Update event) {
		if (!this.players.isEmpty()) {
			this.spawnStuff();
			this.updatePlayers();
			this.moveEnemies(event.getTime());
			this.updateEnemies();
		}
	}

	/**
	 * Cleans up the game and resources.
	 */
	public void shutdown() {
		this.aiLock.lock();
		try {
			this.ai.clear();
		}
		finally {
			this.aiLock.unlock();
		}
		this.playerLock.lock();
		try {
			this.players.clear();
		}
		finally {
			this.playerLock.unlock();
		}
		this.points.clear();
	}

	private void spawnStuff() {
		final int SPAWNS_PER_UPDATE = 2;
		this.aiLock.lock();
		try {
			for (int i = 0; i < Math.random() * SPAWNS_PER_UPDATE; ++i) {
				if ((this.spawnedThisRound < MatchDirector.maxAI(this.round))
						|| this.suddenDeath) {
					Mascot foe = new Mascot("Enemy");
					foe.setUsername("Enemy");
					this.ai.add(foe);
					this.spawnedThisRound++;
					// setup
					Location loc = (Location) foe.getComponent("Location");
					Location tmp = new Location();
					tmp.setX(loc.getX());
					tmp.setY(loc.getY());
					float x, y;
					x = this.rng.nextFloat() * MatchDirector.MAP_WIDTH
							+ MatchDirector.MIN_MAP_X;
					y = this.rng.nextFloat() * MatchDirector.MAP_HEIGHT
							+ MatchDirector.MIN_MAP_Y;
					int tries = 0;
					while (!this.parent.map.isWalkable(Math.round(x),
							Math.round(y))) {
						x = this.rng.nextFloat() * MatchDirector.MAP_WIDTH
								+ MatchDirector.MIN_MAP_X;
						y = this.rng.nextFloat() * MatchDirector.MAP_HEIGHT
								+ MatchDirector.MIN_MAP_Y;
						tmp.setX(x);
						tmp.setY(y);
						++tries;
						if (tries > 100) {
							Logging.warning("Match Director",
									"Trying too much to spawn an enemy.");
							break;
						}
					}
					loc.setX(tmp.getX());
					loc.setY(tmp.getY());

					// alert players
					EntitySpawned es = new EntitySpawned();
					Vect pos = new Vect();
					pos.x = loc.getX();
					pos.y = loc.getY();
					EntityInfo ei = new EntityInfo();
					ei.name = foe.getUsername();
					ei.uniqueID = foe.getName();
					ei.colorIndex = 0;
					es.entity = ei;
					es.position = pos;

					this.parent.broadcast(es);
				}
				else {
					break;
				}
			}
		}
		finally {
			this.aiLock.unlock();
		}
	}

	private void updateEnemies() {
		ArrayList<MoveInfo> moves = new ArrayList<>();

		for (Mascot m : this.ai) {
			MoveInfo moveInfo = new MoveInfo();
			EntityInfo info = new EntityInfo();
			info.name = m.getUsername();
			info.uniqueID = m.getName();
			info.colorIndex = m.getColor();
			moveInfo.entity = info;
			Location loc = (Location) m.getComponent(Location.TYPE_NAME);
			Vect pos = new Vect();
			if (loc != null) {
				pos.x = Math.round(loc.getX());
				pos.y = Math.round(loc.getY());
			}
			else {
				Logging.warning("Match Director", "Null location component.");
				break;
			}
			moveInfo.position = pos;
			moveInfo.facing = this.facing.get(m);
			moveInfo.facing = "up";
			moves.add(moveInfo);
		}
		MoveSet moveSet = new MoveSet();
		MoveInfo[] moveArray = new MoveInfo[moves.size()];
		moveSet.moves = moves.toArray(moveArray);
		moveSet.moveCount = moves.size();

		this.parent.broadcast(moveSet);
	}

	private void updatePlayers() {
		ArrayList<MoveInfo> moves = new ArrayList<>();

		for (Mascot m : this.players) {
			MoveInfo moveInfo = new MoveInfo();
			EntityInfo info = new EntityInfo();
			info.name = m.getUsername();
			info.uniqueID = m.getName();
			info.colorIndex = m.getColor();
			moveInfo.entity = info;
			Location loc = (Location) m.getComponent(Location.TYPE_NAME);
			Vect pos = new Vect();
			if (loc != null) {
				pos.x = Math.round(loc.getX());
				pos.y = Math.round(loc.getY());
			}
			else {
				Logging.warning("Match Director", "Null location component.");
				break;
			}
			moveInfo.position = pos;
			moveInfo.facing = this.facing.get(m);
			moves.add(moveInfo);
		}
		MoveSet moveSet = new MoveSet();
		MoveInfo[] moveArray = new MoveInfo[moves.size()];
		moveSet.moves = moves.toArray(moveArray);
		moveSet.moveCount = moves.size();

		this.parent.broadcast(moveSet);
	}
}
