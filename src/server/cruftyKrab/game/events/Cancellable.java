package cruftyKrab.game.events;

/**
 * Allows event to be canceled so that it is not executed later. A canceled
 * event should not be executed by the server but will be passed to other
 * listeners.
 *
 * @author Ches Burks
 *
 */
public interface Cancellable {

	/**
	 * Gets the cancellation state of this event.
	 *
	 * @return true if this event is cancelled
	 */
	public boolean isCancelled();

	/**
	 * Sets the cancellation state of this event.
	 *
	 * @param cancel true if you wish to cancel this event, false if it is to be
	 *            set to not canceled
	 */
	public void setCancelled(boolean cancel);
}
