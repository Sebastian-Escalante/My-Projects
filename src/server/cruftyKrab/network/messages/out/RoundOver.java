package cruftyKrab.network.messages.out;

/**
 * A round has ended.
 *
 * @author Ches Burks
 *
 */
public class RoundOver {
	/**
	 * The type of event (Equal to this class name, so it can be parsed).
	 */
	public String eventType = "RoundOver";
	/**
	 * What round just ended.
	 */
	public double roundNumber;
}
