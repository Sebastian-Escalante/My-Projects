package cruftyKrab.ai.pathing;

/**
 * A simple tuple, or pair of values. Compared using both key and value.
 *
 * @author Ches Burks
 */
public class Tuple implements Comparable<Tuple> {

	/**
	 * The value used as the key
	 */
	public final Node key;
	/**
	 * The value mapped to the key
	 */
	public final Node value;

	/**
	 * Create a new tuple.
	 *
	 * @param k the key
	 * @param v the value
	 */
	public Tuple(Node k, Node v) {
		this.key = k;
		this.value = v;
	}

	@Override
	public int compareTo(Tuple o) {
		if (this.key.loc.equals(o.key.loc)
				&& this.value.loc.equals(o.value.loc)) {
			return 0;
		}
		return Math.subtractExact(
				Math.addExact(this.key.value(), this.value.value()),
				Math.addExact(o.key.value(), o.value.value()));
	}
}
