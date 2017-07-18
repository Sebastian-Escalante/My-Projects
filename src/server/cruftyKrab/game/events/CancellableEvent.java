package cruftyKrab.game.events;

import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.event.Event;

/**
 * An event that can be canceled, with properly thread-safe implementation of
 * {@link Cancellable} methods.
 *
 * @author Ches Burks
 *
 */
public class CancellableEvent extends Event implements Cancellable {

	private boolean canceled = false;
	private ReentrantLock cancelLock = new ReentrantLock();

	@Override
	public boolean isCancelled() {
		boolean isCanceled = false;

		this.cancelLock.lock();
		try {
			isCanceled = this.canceled;
		}
		finally {
			this.cancelLock.unlock();
		}
		return isCanceled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelLock.lock();
		try {
			this.canceled = cancel;
		}
		finally {
			this.cancelLock.unlock();
		}
	}
}
