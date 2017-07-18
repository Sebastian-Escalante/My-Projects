package cruftyKrab.game;

import java.awt.Point;
import java.util.concurrent.locks.ReentrantLock;

import com.ikalagaming.entity.component.Component;

/**
 * Stores the location information for an entity in the game.
 *
 * @author Ches Burks
 *
 */
public class Location extends Component {

	/**
	 * The name of the component returned by {@link #getType()}. ( {@value} )
	 */
	public static final String TYPE_NAME = "Location";

	private static final float TWO_PI = (float) (Math.PI * 2);

	/**
	 * Calculates the angle that describes the direction of the vector starting
	 * at the source and going to the destination, in degrees clockwise from
	 * north. North is 0 degrees, and the angle will be {@code 0 <= angle < 360}
	 * . This assumes source and destination have reasonable coordinates.
	 *
	 * @param source the start of the vector
	 * @param dest the end point of the vector
	 * @return the angle needed to go towards the destination if starting at the
	 *         source, as degrees clockwise from north
	 */
	public static float findAngle(final Location source, final Location dest) {
		boolean xNegative = false;
		boolean yNegative = false;
		float width = dest.getX() - source.getX();
		float height = dest.getY() - source.getY();

		if (width < 0) {
			width = -width;
			xNegative = true;
		}
		if (height < 0) {
			height = -height;
			yNegative = true;
		}

		if (width == 0) {
			if (yNegative) {
				return 0;
			}
			return 180;
		}
		if (height == 0) {
			if (xNegative) {
				return 90;
			}
			return 270;
		}

		float angle = (float) Math.toDegrees(Math.atan(width / height));

		if (yNegative) {
			if (xNegative) {
				angle += 180;// third quadrant
			}
			else {
				angle += 90;// second quadrant
			}
		}
		else if (xNegative) {
			angle += 270;// third quadrant
		}

		return angle;
	}

	/**
	 * Calculates the angle that describes the direction of the vector starting
	 * at the source and going to the destination, in degrees clockwise from
	 * north. North is 0 degrees, and the angle will be {@code 0 <= angle < 360}
	 * . This assumes source and destination have reasonable coordinates.
	 *
	 * @param source the start of the vector
	 * @param dest the end point of the vector
	 * @return the angle needed to go towards the destination if starting at the
	 *         source, as degrees clockwise from north
	 */
	public static float findAngleRads(final Location source,
			final Location dest) {
		boolean xNegative = false;
		boolean yNegative = false;
		float width = dest.getX() - source.getX();
		float height = dest.getY() - source.getY();

		if (width < 0) {
			width = -width;
			xNegative = true;
		}
		if (height < 0) {
			height = -height;
			yNegative = true;
		}
		if (width == 0) {
			if (yNegative) {
				return 0.0f;
			}
			return Location.TWO_PI;
		}
		if (height == 0) {
			if (xNegative) {
				return (float) (Math.PI / 2);
			}
			return (float) ((3 * Math.PI) / 2);
		}

		float angle = (float) Math.atan(width / height);

		if (yNegative) {
			if (xNegative) {
				angle += Math.PI;// third quadrant
			}
			else {
				angle += Math.PI / 2;// second quadrant
			}
		}
		else if (xNegative) {
			angle += (3 * Math.PI) / 2;// third quadrant
		}

		return angle;
	}

	/**
	 * Determines the distance between two locations, assuming reasonable values
	 * for position.
	 *
	 * @param a the first location
	 * @param b the second location
	 * @return distance between the locations
	 */
	public static float getDistance(final Location a, final Location b) {
		final float dx = b.getX() - a.getX();
		final float dy = b.getY() - a.getY();
		return (float) Math.sqrt((dx * dx) + (dy * dy));
	}

	private ReentrantLock dataLock = new ReentrantLock();
	/**
	 * The x position
	 */
	private float xPos;
	/**
	 * The y position
	 */
	private float yPos;

	/**
	 * The angle in radians
	 */
	private float angleRad;

	/**
	 * Returns the angle the entity is facing, in degrees.
	 *
	 * @return the angle in deg
	 */
	public int getAngleDeg() {
		float angle = 0.0f;
		this.dataLock.lock();
		try {
			angle = this.angleRad;
		}
		finally {
			this.dataLock.unlock();
		}
		return Math.round((float) Math.toDegrees(angle));
	}

	/**
	 * Returns the angle the entity is facing, in radians.
	 *
	 * @return the angle in rad
	 */
	public float getAngleRad() {
		float angle = 0.0f;
		this.dataLock.lock();
		try {
			angle = this.angleRad;
		}
		finally {
			this.dataLock.unlock();
		}
		return angle;
	}

	/**
	 * Rounds the x and y position to an integer, and returns the tile there.
	 * This should be the tile that most of the player is on.
	 *
	 * @return the point representing the location rounded to integers
	 */
	public Point getTilePos() {
		Point p;
		this.dataLock.lock();
		try {
			p = new Point(Math.round(this.xPos), Math.round(this.yPos));
		}
		finally {
			this.dataLock.unlock();
		}
		return p;
	}

	@Override
	public String getType() {
		return Location.TYPE_NAME;
	}

	/**
	 * Returns the X position of the entity
	 *
	 * @return the X position
	 */
	public float getX() {
		float x = 0.0f;
		this.dataLock.lock();
		try {
			x = this.xPos;
		}
		finally {
			this.dataLock.unlock();
		}
		return x;
	}

	/**
	 * Returns the Y position of the entity
	 *
	 * @return the Y position
	 */
	public float getY() {
		float y = 0.0f;
		this.dataLock.lock();
		try {
			y = this.yPos;
		}
		finally {
			this.dataLock.unlock();
		}
		return y;
	}

	/**
	 * Sets the angle the entity is facing to a specified angle, in degrees.
	 * This should be between -180 and 180 where positive is clockwise and
	 * negative is counterclockwise.
	 *
	 * @param angle the direction in degrees the entity is facing
	 */
	public void setAngleDeg(final int angle) {
		int val = angle;
		if (val < 0) {
			while (val <= -180) {
				val += 180;// make it -2pi <= val <= 2pi
			}
		}
		else {
			while (val >= 180) {
				val -= 180;// make it -2pi <= val <= 2pi
			}
		}
		float fVal = (float) Math.toRadians(val);

		this.dataLock.lock();
		try {
			this.angleRad = fVal;
		}
		finally {
			this.dataLock.unlock();
		}

	}

	/**
	 * Sets the angle the entity is facing to a specified angle, in radians.
	 *
	 * @param angle the direction in radians the entity is facing
	 */
	public void setAngleRad(final float angle) {
		float val = angle;
		if (val < 0) {
			while (val <= -Location.TWO_PI) {
				val += Location.TWO_PI;// make it -2pi <= val <= 2pi
			}
		}
		else {
			while (val >= Location.TWO_PI) {
				val -= Location.TWO_PI;// make it -2pi <= val <= 2pi
			}
		}
		this.dataLock.lock();
		try {
			this.angleRad = val;
		}
		finally {
			this.dataLock.unlock();
		}
	}

	/**
	 * Sets the X position of the entity
	 *
	 * @param x the new X position
	 */
	public void setX(final float x) {
		this.dataLock.lock();
		try {
			this.xPos = x;
		}
		finally {
			this.dataLock.unlock();
		}
	}

	/**
	 * Sets the X position of the entity
	 *
	 * @param y the new Y position
	 */
	public void setY(final float y) {
		this.dataLock.lock();
		try {
			this.yPos = y;
		}
		finally {
			this.dataLock.unlock();
		}
	}
}
