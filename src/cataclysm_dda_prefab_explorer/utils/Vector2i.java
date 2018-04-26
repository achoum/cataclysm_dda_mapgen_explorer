
package cataclysm_dda_prefab_explorer.utils;

public class Vector2i {
	public int x, y;

	public static final Vector2i[] DIRS4 = new Vector2i[] { new Vector2i(1, 0), new Vector2i(0, 1),
			new Vector2i(-1, 0), new Vector2i(0, -1) };

	public Vector2i(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vector2i() {
	}

}
