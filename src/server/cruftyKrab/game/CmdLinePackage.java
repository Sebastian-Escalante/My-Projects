package cruftyKrab.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.ikalagaming.event.EventHandler;
import com.ikalagaming.event.EventManager;
import com.ikalagaming.event.Listener;
import com.ikalagaming.gui.console.events.ConsoleCommandEntered;
import com.ikalagaming.gui.console.events.ConsoleMessage;
import com.ikalagaming.gui.console.events.ReportUnknownCommand;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.packages.Package;
import com.ikalagaming.packages.PackageManager;
import com.ikalagaming.packages.events.PackageCommandSent;

import cruftyKrab.game.events.Shutdown;

/**
 * Allows IO with standard in/out so the server can be interacted with.
 *
 * @author Ches Burks
 *
 */
public class CmdLinePackage implements Package, Listener {

	/**
	 * The name that is returned by {@link #getName()}. This is made public to
	 * ease development.
	 */
	public static final String packageName = "Command Input";
	private static final double version = 0.1;

	private Set<Listener> listeners;
	private StdInReader reader;

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
		return CmdLinePackage.packageName;
	}

	@Override
	public double getVersion() {
		return CmdLinePackage.version;
	}

	/**
	 * Called when a command event is sent.
	 *
	 * @param event the command sent
	 */
	@EventHandler
	public void onCommand(PackageCommandSent event) {
		if (!event.getTo().equalsIgnoreCase(this.getName())) {
			return;
		}
		if (event.getCommand().equals("shutdown")) {
			EventManager.getInstance().fireEvent(new Shutdown());
		}
	}

	/**
	 * When a console message is sent, prints it.
	 *
	 * @param event the event that was received
	 */
	@EventHandler
	public void onConsoleMessage(ConsoleMessage event) {
		if (!PackageManager.getInstance().isEnabled(this)) {
			System.err
					.println("Command line is disabled! Cannot print message '"
							+ event.getMessage() + "'");
			return;// Don't try to log things if it is disabled
		}
		System.out.println(event.getMessage());
	}

	@Override
	public boolean onDisable() {
		this.reader.terminate();
		return true;
	}

	@Override
	public boolean onEnable() {
		this.reader = new StdInReader();
		this.reader.start();
		return true;
	}

	@Override
	public boolean onLoad() {
		PackageManager.getInstance().registerCommand("shutdown", this);
		return true;
	}

	/**
	 * Appends a message stating the last command was incorrect and a help
	 * message informing the user of the help command.
	 *
	 * @param event the command that was reported as unknown
	 */
	@EventHandler
	public void onReportUnknownCommand(ReportUnknownCommand event) {
		String message = "Unknown command" + " '" + event.getCommand() + "'. "
				+ "For a list of available commands, type" + " '" + "help"
				+ "'";
		if (!PackageManager.getInstance().isEnabled(this)) {
			System.err
					.println("Command line is disabled! Cannot print message '"
							+ message + "'");
			return;// Don't try to log things if it is disabled
		}
		System.out.println(message);
	}

	/**
	 * Shuts down the reader
	 *
	 * @param event the event received
	 */
	@EventHandler
	public void onShutdown(Shutdown event) {
		this.reader.terminate();
	}

	@Override
	public boolean onUnload() {
		PackageManager.getInstance().unregisterPackageCommands(this);
		return true;
	}

}

/**
 * Listens for new connections and dispatches connections when available.
 *
 * @author Ches Burks
 *
 */
class StdInReader extends Thread {

	private static final int TIMEOUT = 1000;
	private boolean running;
	private Object waitObject;

	/**
	 * Creates and starts the thread. It will begin attempting to dispatch
	 * events immediately if there are any available.
	 */
	public StdInReader() {
		this.setName("Standard Input Reader");
		this.running = true;
		this.waitObject = new Object();
	}

	/**
	 * Checks for Strings in the queue, and logs them if possible. Does not do
	 * anything if {@link #terminate()} has been called.
	 *
	 */
	@Override
	public void run() {
		BufferedReader in =
				new BufferedReader(new InputStreamReader(System.in));
		while (this.running) {
			String line = "";
			synchronized (this.waitObject) {
				try {
					// block this thread until an item is added
					this.waitObject.wait(StdInReader.TIMEOUT);
				}
				catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
			}
			try {
				if (in.ready()) {
					line = in.readLine();
				}
				else {
					continue;
				}
			}
			catch (@SuppressWarnings("unused") IOException e) {
				Logging.warning(CmdLinePackage.packageName,
						"Error reading standard input");
			}
			ConsoleCommandEntered cmd = new ConsoleCommandEntered(line);
			EventManager.getInstance().fireEvent(cmd);
		}
	}

	/**
	 * Stops the thread from executing its run method in preparation for
	 * shutting down the thread.
	 */
	public synchronized void terminate() {
		synchronized (this.waitObject) {
			// Wake the thread up as there is now an event
			this.waitObject.notify();
		}
		this.running = false;
	}

}
