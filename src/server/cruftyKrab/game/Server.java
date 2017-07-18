package cruftyKrab.game;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.java_websocket.drafts.Draft_17;

import com.ikalagaming.event.EventHandler;
import com.ikalagaming.event.EventManager;
import com.ikalagaming.event.Listener;
import com.ikalagaming.logging.LogLevel;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.packages.Package;
import com.ikalagaming.packages.PackageManager;

import cruftyKrab.game.events.Shutdown;
import cruftyKrab.game.lobby.LobbyPackage;
import cruftyKrab.network.WSServer;

/**
 * The entrypoint of the program, as well as container of the main loop.
 *
 * @author Ches Burks
 *
 */
public class Server implements Package, Listener {

	/**
	 * The default port the server will listen for connections on.
	 */
	public static final int SERVER_PORT = 12647;

	/**
	 * The name that is returned by {@link #getName()}. This is made public to
	 * ease development.
	 */
	public static final String packageName = "Server";
	private static final double version = 0.2;

	/**
	 * Starts the main loop.
	 *
	 * @param args arguments to the main method.
	 */
	public static void main(String[] args) {
		Server s = new Server();
		s.start();
	}

	private Set<Listener> listeners;

	private WSServer server;

	/**
	 * Sets up a new game sever and loads systems.
	 */
	public Server() {
		// begin TRUST_ME_THIS_IS_USEFUL
		EventManager.getInstance();
		Logging.setLogLevel(LogLevel.FINER);// set to INFO before deployment
		PackageManager.getInstance();
		// end TRUST_ME_THIS_IS_USEFUL
	}

	@Override
	public Set<Listener> getListeners() {
		if (this.listeners == null) {
			this.listeners = new HashSet<>();
			this.listeners.add(this);
		}
		return this.listeners;
	}

	@Override
	public String getName() {
		return Server.packageName;
	}

	@Override
	public double getVersion() {
		return Server.version;
	}

	@Override
	public boolean onDisable() {
		this.server.closeAll();
		try {
			this.server.stop();
		}
		catch (IOException e) {
			Logging.finer(Server.packageName,
					"Exception closing WebSocket server.");
			e.printStackTrace(System.err);
		}
		catch (InterruptedException e) {
			Logging.finer(Server.packageName,
					"Interrupted thread closing WebSocket Server.");
			e.printStackTrace(System.err);
		}
		this.server = null;
		PackageManager.getInstance().disable(LobbyPackage.packageName);
		PackageManager.getInstance().disable(CmdLinePackage.packageName);
		return true;
	}

	@Override
	public boolean onEnable() {
		Logging.finest(Server.packageName, "Starting WebSocket server...");
		try {
			this.server = new WSServer(Server.SERVER_PORT, new Draft_17());
			this.server.start();
		}
		catch (@SuppressWarnings("unused") UnknownHostException e) {
			Logging.warning(Server.packageName,
					"Error starting websocket server");
		}

		PackageManager.getInstance().enable(LobbyPackage.packageName);
		PackageManager.getInstance().enable(CmdLinePackage.packageName);
		return true;
	}

	@Override
	public boolean onLoad() {
		PackageManager.getInstance().loadPackage(new LobbyPackage());
		PackageManager.getInstance().loadPackage(new CmdLinePackage());
		return true;
	}

	/**
	 * Unloads this package.
	 *
	 * @param event the event that was received.
	 */
	@EventHandler
	public void onShutdown(Shutdown event) {
		PackageManager.getInstance().unloadPackage(Server.packageName);
	}

	@Override
	public boolean onUnload() {
		PackageManager.getInstance().unloadPackage(LobbyPackage.packageName);
		PackageManager.getInstance().unloadPackage(CmdLinePackage.packageName);

		// shut down everything else
		GracefulShutdown g = new GracefulShutdown();
		g.start();
		return true;
	}

	/**
	 * Loads this package up in the package manager, which basically means the
	 * server will now stay on until stopped manually.
	 */
	public void start() {
		PackageManager.getInstance().loadPackage(this);
		PackageManager.getInstance().enable(this);
	}
}
