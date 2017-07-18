package cruftyKrab.game;

import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.entity.component.Component;

/**
 * Stores the score for an entity.
 *
 * @author Ches Burks
 *
 */
public class Score extends Component {

	/**
	 * The name of the component returned by {@link #getType()}. ( {@value} )
	 */
	public static final String TYPE_NAME = "Score";

	private int score;
	private ReentrantLock scoreLock = new ReentrantLock();

	/**
	 * Constructs a new score component with zero score.
	 */
	public Score() {
		this.score = 0;
	}

	/**
	 * Increase the score by a given amount.
	 *
	 * @param quantity how many points to add
	 */
	public void add(final int quantity) {
		if (quantity <= 0) {
			return;
		}
		this.scoreLock.lock();
		try {
			this.score += quantity;
		}
		finally {
			this.scoreLock.unlock();
		}
	}

	/**
	 * Returns the score for the entity
	 *
	 * @return how many points the entity has
	 */
	public int getScore() {
		int amt = 0;
		this.scoreLock.lock();
		try {
			amt = this.score;
		}
		finally {
			this.scoreLock.unlock();
		}
		return amt;
	}

	@Override
	public String getType() {
		return Score.TYPE_NAME;
	}

	/**
	 * Decrease the score by a given amount.
	 *
	 * @param quantity how many points to remove
	 */
	public void remove(final int quantity) {
		if (quantity >= 0) {
			return;
		}
		this.scoreLock.lock();
		try {
			this.score -= quantity;
		}
		finally {
			this.scoreLock.unlock();
		}
	}
}
