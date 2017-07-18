package cruftyKrab.game.lobby;

import java.security.SecureRandom;

import com.ikalagaming.event.EventManager;

import cruftyKrab.game.events.Update;

/**
 * Update the Match AI every once in a while.
 *
 * @author Ches Burks
 *
 */
public class UpdateThread extends Thread {

	/**
	 * A list of prime numbers, whose average is 1009, which is also a prime
	 * number and also in the list.
	 */
	public static final long[] TIMES =
			{829, 911, 929, 947, 953, 967, 971, 977, 983, 991, 997, 1009, 1013,
					1019, 1021, 1031, 1033, 1039, 1087, 1091, 1093, 1103, 1213};
	private static SecureRandom secRNG = new SecureRandom();

	/**
	 * Returns a random time to wait.
	 *
	 * @return a wait time, in ms
	 */
	private static long getDelay() {
		return UpdateThread.TIMES[(int) (UpdateThread.secRNG.nextFloat()
				* UpdateThread.TIMES.length)];
	}

	private EventManager manager;
	private boolean running;

	private Object waitObj;

	/**
	 * Creates an update thread for a given manager.
	 *
	 * @param manag the event manager to fire updates to
	 */
	public UpdateThread(EventManager manag) {
		this.setName("UpdateThread");
		this.manager = manag;
		this.waitObj = new Object();
		this.running = true;
	}

	@Override
	public void run() {
		long delay = 1;
		while (this.running) {
			this.manager.fireEvent(new Update(delay));
			delay = UpdateThread.getDelay();
			synchronized (this.waitObj) {
				try {
					this.waitObj.wait(delay);
				}
				catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}

	/**
	 * Tell the thread to stop running asap.
	 */
	public synchronized void terminate() {
		this.running = false;
	}

}
