package cruftyKrab.game;

import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.entity.component.Component;

/**
 * Counts how many kills an entity has made.
 *
 * @author Ches Burks
 *
 */
public class Kills extends Component {

	/**
	 * The name of the component returned by {@link #getType()}. ( {@value} )
	 */
	public static final String TYPE_NAME = "Kills";

	private int numKills;
	private ReentrantLock killsLock = new ReentrantLock();

	/**
	 * Constructs a new kills component with zero count.
	 */
	public Kills() {
		this(0);
	}

	/**
	 * Constructs a new kills component with specified count
	 *
	 * @param kills the starting number of kills
	 */
	public Kills(final int kills) {
		this.numKills = kills;
	}

	/**
	 * Returns the kill count for the entity
	 *
	 * @return how many kills they have recorded
	 */
	public int getCount() {
		int amt = 0;
		this.killsLock.lock();
		try {
			amt = this.numKills;
		}
		finally {
			this.killsLock.unlock();
		}
		return amt;
	}

	@Override
	public String getType() {
		return Kills.TYPE_NAME;
	}

	/**
	 * Increase the kill count by one.
	 */
	public void increment() {
		this.killsLock.lock();
		try {
			++this.numKills;
		}
		finally {
			this.killsLock.unlock();
		}
	}

	/**
	 * Increase the kill count by a given amount.
	 *
	 * @param quantity how many kills to increase by
	 */
	public void increment(final int quantity) {
		if (quantity <= 0) {
			return;
		}
		this.killsLock.lock();
		try {
			this.numKills += quantity;
		}
		finally {
			this.killsLock.unlock();
		}
	}
}
