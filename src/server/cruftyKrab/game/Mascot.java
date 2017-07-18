package cruftyKrab.game;

import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.entity.Player;

/**
 * Represents a mascot in the game. Can be either played by a person or by AI.
 *
 * @author Ches Burks
 *
 */
public class Mascot extends Player {

	/**
	 * The name of the mascot, as requested by the player (if played by a
	 * person). {@code name} is reserved, unique, and returned by
	 * {@link Mascot#getName()}. The username isn't necessarily guaranteed to be
	 * unique, but the name will be unique.
	 *
	 * It is recommended to use {@code name} instead of {@code username} for
	 * internal representation, but username for displaying in-game name.
	 */
	private String username;

	private ReentrantLock nameLock;

	private int color;
	private ReentrantLock colorLock;

	/**
	 * Constructs a mascot and gives it some essential components.
	 */
	public Mascot() {
		this("Mascot");
	}

	/**
	 * Constructs a mascot and gives it some essential components.
	 *
	 * @param nameHint the the base name
	 */
	public Mascot(final String nameHint) {
		super(nameHint); // adds health and inventory components
		this.addComponent(new Kills());
		this.addComponent(new Score());
		this.addComponent(new Location());
		this.nameLock = new ReentrantLock();
		this.colorLock = new ReentrantLock();
		this.color = MascotColor.MAROON.getIndex();
	}

	/**
	 * Returns the mascots color index. See {@link MascotColor#getIndex()} for a
	 * better explanation of this number.
	 *
	 * @return the color
	 */
	public int getColor() {
		this.colorLock.lock();
		try {
			return this.color;
		}
		finally {
			this.colorLock.unlock();
		}
	}

	/**
	 * Returns the username of the player. Note this is different than the name
	 * returned by {@link Mascot#getName()}, and is not necessarily guaranteed
	 * to be unique.
	 *
	 * Because it is not checked for uniqueness, it is recommended to use the
	 * {@link Mascot#getName()} instead for internal representation, but
	 * username for displaying in-game name.
	 *
	 * @return the mascots username
	 */
	public String getUsername() {
		String ret = this.getName();
		this.nameLock.lock();
		try {
			ret = this.username;
		}
		finally {
			this.nameLock.unlock();
		}
		return ret;
	}

	/**
	 * Sets the mascots color (frame) index. See {@link MascotColor#getIndex()}
	 * for a better explanation of this number.
	 *
	 * @param newColorInd the color to set
	 */
	public void setColor(final int newColorInd) {
		this.colorLock.lock();
		try {
			this.color = newColorInd;
		}
		finally {
			this.colorLock.unlock();
		}
	}

	/**
	 * Sets the username of the player. Note this is different than the name
	 * returned by {@link Mascot#getName()}, and is not necessarily guaranteed
	 * to be unique.
	 *
	 * Because it is not checked for uniqueness, it is recommended to use the
	 * {@link Mascot#getName()} instead of {@link Mascot#getUsername()} for
	 * internal representation, but username for displaying in-game name.
	 *
	 * @param newUsername the new username for the mascot
	 */
	public void setUsername(final String newUsername) {
		this.nameLock.lock();
		try {
			this.username = newUsername;
		}
		finally {
			this.nameLock.unlock();
		}
	}
}
