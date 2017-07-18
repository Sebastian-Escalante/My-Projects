package cruftyKrab.game;

/**
 * The colors of a rev's vest. This is using the sprite sheet version 1.
 *
 * @author Ches Burks
 *
 */
public enum MascotColor {
	/**
	 * Maroon vest
	 */
	MAROON(0),
	/**
	 * Blue vest
	 */
	BLUE(1),
	/**
	 * Green vest
	 */
	GREEN(2),
	/**
	 * Yellow vest
	 */
	YELLOW(3),
	/**
	 * Aqua vest
	 */
	AQUA(4),
	/**
	 * Grey vest
	 */
	GREY(5),
	/**
	 * Mint vest
	 */
	MINT(6),
	/**
	 * Pink vest
	 */
	PINK(7),
	/**
	 * Purple vest
	 */
	PURPLE(8),
	/**
	 * Red vest
	 */
	RED(9);

	/**
	 * The index of the color on the sprite sheet. This is some number {@code n}
	 * such that {@code n*SPRITE_SIZE} is the Y position of the top of the
	 * player's sprite on the sprite sheet. In other words, if the sprite sheet
	 * is {@code h} sprites high, then {@code n} is a number such that
	 * {@code 0 <= n <= h-1}, where {@code n} corresponds to exactly which
	 * sprite is used for that player.
	 */
	final int index;

	/**
	 * Creates a {@link MascotColor} with the given index of the sprite sheet.
	 *
	 * @param indexNum the index of the color on the sprite sheet
	 */
	private MascotColor(final int indexNum) {
		this.index = indexNum;
	}

	/**
	 * Returns the index of the color. This is some number {@code n} such that
	 * {@code n*SPRITE_SIZE} is the Y position of the top of the player's sprite
	 * on the sprite sheet. In other words, if the sprite sheet is {@code h}
	 * sprites high, then {@code n} is a number such that {@code 0 <= n <= h-1},
	 * where {@code n} corresponds to exactly which sprite is used for that
	 * player.
	 *
	 * @return the index of a color
	 */
	public int getIndex() {
		return this.index;
	}
}
