package cruftyKrab.network.messages.out;

import cruftyKrab.network.messages.ScoreInfo;

/**
 * A game has ended.
 *
 * @author Ches Burks
 *
 */
public class GameOver {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "GameOver";

	/**
	 * How many scores are in the array of scores.
	 */
	public double numScores;

	/**
	 * A list of scores for players.
	 */
	public ScoreInfo[] scores;
}
