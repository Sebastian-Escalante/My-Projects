package cruftyKrab.ai;

/**
 * The state of an AI actor.
 *
 * @author Ches Burks
 *
 */
public enum State {
	/**
	 * The start state.
	 */
	START,
	/**
	 * The AI is looking for a player to attack.
	 */
	FINDING_TARGET,
	/**
	 * The AI is calculating the path to a player.
	 */
	PATHFINDING,
	/**
	 * The AI is moving to a player.
	 */
	MOVING,
	/**
	 * The AI is attacking a player.
	 */
	ATTACKING,
	/**
	 * The AI is dead.
	 */
	DEAD,
	/**
	 * There was some error. Ideally should never be this.
	 */
	INVALID;
}
