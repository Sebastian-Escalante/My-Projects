package cruftyKrab.game;

import com.ikalagaming.event.EventManager;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.packages.PackageManager;

/**
 * Cleans up the event manager, packages, etc. in the background while the main
 * thread finishes its shutdown calls.
 *
 * @author Ches Burks
 *
 */
class GracefulShutdown extends Thread {

	private Object waitObject = new Object();

	@Override
	public void run() {
		try {
			/*
			 * Wait a couple seconds to give the server time to shut its stuff
			 * down.
			 */
			synchronized (this.waitObject) {
				this.waitObject.wait(2000);
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
		PackageManager.destoryInstance();
		EventManager.destoryInstance();
		Logging.destory();
	}
}