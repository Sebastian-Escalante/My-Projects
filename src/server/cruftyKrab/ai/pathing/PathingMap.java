package cruftyKrab.ai.pathing;

import java.util.BitSet;

import com.ikalagaming.logging.Logging;

import tiled.core.Map;
import tiled.core.TileLayer;

/**
 * A 2d representation of the map as a grid of tiles that are either walkable or
 * not. This is used to represent the map in the pathfinding code as that is all
 * the information it needs.
 *
 * @author Ches Burks
 *
 */
public class PathingMap {

	private final BitSet tiles;

	private final int width;
	private final int height;

	/**
	 * Creates a new map with the given width and height. Layer 0 is considered
	 * walkable, and layer 1 is considered not walkable.
	 *
	 * @param tiledMap the Tiled map to base this map on
	 */
	public PathingMap(final Map tiledMap) {
		this.width = tiledMap.getWidth();
		this.height = tiledMap.getHeight();
		int size;
		try {
			size = Math.multiplyExact(this.width, this.height);
			if (size < 0) {
				Logging.severe("Pathfinding", "Map has negative dimensions");
				size = 1;
			}
		}
		catch (ArithmeticException e) {
			e.printStackTrace(System.err);
			size = 1000;
			Logging.severe("Pathfinding", "Map is too large");
		}

		this.tiles = new BitSet(size);

		final int layerCount = tiledMap.getLayerCount();
		boolean hasTwoLayers;
		switch (layerCount) {
		case 0:
			Logging.severe("Pathfinding", "Map has no layers");
			hasTwoLayers = false;
			break;
		case 1:
			Logging.warning("Pathfinding", "Map only has one layer");
			hasTwoLayers = false;
			break;
		case 2:
			hasTwoLayers = true;
			break;
		default:
			hasTwoLayers = true;
			Logging.warning("Pathfinding",
					"Map has more layers than necessary");
			break;
		}

		this.calcMap(tiledMap, size, hasTwoLayers);

	}

	private void calcMap(final Map tiledMap, int size, boolean hasTwoLayers) {
		if (!(tiledMap.getLayer(1) instanceof TileLayer)) {
			Logging.severe("Pathfinding",
					"Cannot pull tile information from map");
			return;
		}
		TileLayer layerTwo = (TileLayer) tiledMap.getLayer(1);
		if (!hasTwoLayers) {
			// skip all this
			this.tiles.set(0, size);// set whole map to walkable
			return;
		}
		for (int y = 0; y < this.height; ++y) {
			for (int x = 0; x < this.width; ++x) {
				if (!layerTwo.contains(x, y)) {
					Logging.severe("Pathfinding",
							"Map layers are different sizes");
					return;
				}
				if (layerTwo.getTileAt(x, y) == null) {
					// there is no collision here
					this.tiles.set(y * this.width + x);// set to true
				}
			}
		}
	}

	/**
	 * Returns the height of the map
	 *
	 * @return the map height in tiles
	 */
	public int getHeight() {
		return this.height;
	}

	/**
	 * Returns the width of the map
	 *
	 * @return the map width in tiles
	 */
	public int getWidth() {
		return this.width;
	}

	/**
	 * Returns true if the tile at (x,y) is walkable. X and Y should be positive
	 * indexes, starting at 0, and less than the size of the map. Invalid
	 * indices results in a return value of {@code false}.
	 *
	 * @param x the x index of the tile
	 * @param y they y index of the tile
	 * @return true if the tile is walkable, false if it is not
	 */
	public boolean isWalkable(final int x, final int y) {
		if (x >= this.width) {
			return false;
		}
		if (y >= this.height) {
			return false;
		}
		if (x < 0 || y < 0) {
			return false;
		}
		int location;
		try {
			int rowTemp = Math.multiplyExact(y, this.width);
			location = Math.addExact(rowTemp, x);
			if (location < 0) {
				return false;
			}
		}
		catch (ArithmeticException e) {
			e.printStackTrace(System.err);
			return false;
		}
		return this.tiles.get(location);
	}
}
