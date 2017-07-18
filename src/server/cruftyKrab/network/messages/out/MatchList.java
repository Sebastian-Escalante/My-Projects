package cruftyKrab.network.messages.out;

import cruftyKrab.network.messages.MatchInfo;

/**
 * A list of matches.
 *
 * @author Ches Burks
 *
 */
public class MatchList {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "MatchList";
	/**
	 * How many matches are in the matches list.
	 */
	public double matchCount;
	/**
	 * The list of matches going on.
	 */
	public MatchInfo[] matches;
}
