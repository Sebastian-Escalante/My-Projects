package cruftyKrab.game.lobby;

import com.google.gson.Gson;
import com.ikalagaming.event.EventHandler;
import com.ikalagaming.event.EventManager;
import com.ikalagaming.event.Listener;
import com.ikalagaming.gui.console.events.ConsoleCommandEntered;
import com.ikalagaming.logging.Logging;

import cruftyKrab.game.events.EntityDiedEvent;
import cruftyKrab.game.events.JoinMatch;
import cruftyKrab.game.events.MatchOver;
import cruftyKrab.game.events.PlayerConnected;
import cruftyKrab.game.events.PlayerUpdate;
import cruftyKrab.network.Connection;
import cruftyKrab.network.events.MessageReceived;
import cruftyKrab.network.messages.EntityDied;
import cruftyKrab.network.messages.in.PosUpdate;

/**
 * The lobby package listener that is registered with the static event system.
 *
 * @author Ches Burks
 *
 */
public class LobbyListener implements Listener {

	private LobbyPackage parent;

	/**
	 * Create a listener for the lobby given its owner.
	 *
	 * @param parentLobby the parent to listen to events for and modify
	 */
	public LobbyListener(final LobbyPackage parentLobby) {
		this.parent = parentLobby;
	}

	/**
	 * Transfers a match join event to the appropriate match
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onJoinMatch(JoinMatch event) {
		Logging.finer(LobbyPackage.packageName,
				"Player joining match " + event.getMatchID() + ".");
		Match m = this.parent.getMatch(event.getMatchID());
		if (m == null) {
			return;
		}
		if (m.getEventManager() != EventManager.getInstance()) {
			m.getEventManager().fireEvent(event);
		}
	}

	/**
	 * Kick all players to the lobby from that match, and destroy the match.
	 *
	 * @param event the event received
	 */
	@EventHandler
	public void onMatchOver(MatchOver event) {
		this.parent.getMatch(event.getID()).shutdown();
		this.parent.destoryMatch(event.getID());
	}

	/**
	 * Transfers the event to the match listener.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onMessageReceived(MessageReceived event) {
		if (event.getMessage().contains("\"eventName\":\"posUpdate\"")) {
			Gson g = event.getSocket().getGson();
			PosUpdate pu = g.fromJson(event.getMessage(), PosUpdate.class);
			PlayerUpdate playerUpdate = new PlayerUpdate(pu.xPosition,
					pu.yPosition, pu.facing, event.getSocket());
			event.getSocket().getManager().fireEvent(playerUpdate);
			return;
		}

		if (event.getMessage().contains("\"eventName\":\"EntityDied\"")) {
			Gson g = event.getSocket().getGson();
			EntityDied ed = g.fromJson(event.getMessage(), EntityDied.class);
			EntityDiedEvent edEvent = new EntityDiedEvent(ed.uniqueID,
					event.getSocket(), ed.points);
			event.getSocket().getManager().fireEvent(edEvent);
			return;
		}

		if (event.getSocket().getManager() != EventManager.getInstance()) {
			event.getSocket().getManager().fireEvent(event);
		}
	}

	/**
	 * Allows for a sudden death mode, freezing enemies.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onPackageCommand(ConsoleCommandEntered event) {
		if (event.getCommand().equals("suddenDeath")) {
			Logging.finer(LobbyPackage.packageName, "Toggling sudden death");
			this.parent.suddenDeath();
		}
		else if (event.getCommand().equals("freeze")) {
			Logging.finer(LobbyPackage.packageName, "Toggling freeze");
			this.parent.freeze();
		}
	}

	/**
	 * Adds new players to the lobby.
	 *
	 * @param event the event that was received.
	 */
	@EventHandler
	public void onPlayerConnected(PlayerConnected event) {
		Logging.finer(LobbyPackage.packageName,
				"Player connected event received! Adding to lobby.");
		Connection c = event.getConnection();
		this.parent.addToLobby(c);
		this.parent.addToMatch(c);
	}

}
