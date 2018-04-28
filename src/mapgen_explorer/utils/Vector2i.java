
package mapgen_explorer.utils;

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

	@Override
	public boolean equals(Object p) {
		if (p instanceof Vector2i)
			return equals((Vector2i) p);
		else
			return false;
	}

	public boolean equals(Vector2i p) {
		return p.x == x && p.y == y;
	}

	public void set(Vector2i p) {
		this.x = p.x;
		this.y = p.y;
	}

	public void set(int x, int y) {
		this.x = x;
		this.y = y;
	}

}
