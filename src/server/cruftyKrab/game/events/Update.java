package cruftyKrab.game.events;

import com.ikalagaming.event.Event;

/**
 * Perform updates in a match, such as AI calculations, or message dispatch.
 *
 * @author Ches Burks
 *
 */
public class Update extends Event {

	private final long t;

	/**
	 * An update after a pause of so many ms.
	 *
	 * @param time the time in ms since the last update
	 */
	public Update(final long time) {
		this.t = time;
	}

	/**
	 * Returns the time in ms since the last update
	 *
	 * @return the time in ms
	 */
	public long getTime() {
		return this.t;
	}
}
