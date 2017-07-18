package cruftyKrab.game;

/**
 * A list of attacks that an entity can use.
 *
 * @author Ches Burks
 *
 */
public enum Attack {
	/**
	 * A melee attack against one target
	 */
	BITE(1.2f, 1, 0),
	/**
	 * A melee attack against multiple targets in the direction of use
	 */
	SCRATCH(2.1f, 1, 3),
	/**
	 * A ranged attack against one target
	 */
	FIREBALL(3.2f, 4, 5),
	/**
	 * A melee attack against everything in the vicinity
	 */
	EXPLOSION(3.6f, 2, 10);

	/**
	 * The damage multiplier for an attack, as a decimal percent. That is, 1.0f
	 * is 100% damage, 0.1f is 10% damage, etc.
	 */
	private final float damageMult;

	/**
	 * How far the attack can reach
	 */
	private final int attackRange;

	/**
	 * How long to wait between power uses in seconds
	 */
	private final int cooldownTime;

	/**
	 * Constructs a new attack enum element.
	 *
	 * The damage multiplier for an attack, is a float representing a decimal
	 * percent. That is, 1.0f is 100% damage, 0.1f is 10% damage, etc.
	 *
	 * @param damageMultiplier the decimal percent multiplier of player damage
	 *            to deal
	 * @param range how far in units the attack reaches
	 * @param cooldown time in seconds between uses
	 */
	private Attack(final float damageMultiplier, final int range,
			final int cooldown) {
		this.damageMult = damageMultiplier;
		this.attackRange = range;
		this.cooldownTime = cooldown;
	}

	/**
	 * Returns an int representing how far away from the user the power can
	 * reach. 1 unit is roughly one tile or the size of the player away, so
	 * basically arms reach.
	 *
	 * @return the attackRange how far the power reaches
	 */
	public int getAttackRange() {
		return this.attackRange;
	}

	/**
	 * Returns the time in seconds required to wait after a power is used before
	 * it can be used again.
	 *
	 * @return the cooldownTime seconds between uses
	 */
	public int getCooldownTime() {
		return this.cooldownTime;
	}

	/**
	 * Returns the damage multiplier for an attack, as a float representing a
	 * decimal percent. That is, 1.0f is 100% damage, 0.1f is 10% damage, etc.
	 *
	 * @return the damageMult the decimal percent multiplier of player damage to
	 *         deal
	 */
	public float getDamageMult() {
		return this.damageMult;
	}
}
