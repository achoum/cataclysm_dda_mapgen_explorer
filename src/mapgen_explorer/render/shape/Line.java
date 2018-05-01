
package mapgen_explorer.render.shape;

import mapgen_explorer.utils.Geometry;
import mapgen_explorer.utils.Geometry.PointCallback;
import mapgen_explorer.utils.Vector2i;

public class Line extends AbstractShape {
	public int x1;
	public int y1;
	public int x2;
	public int y2;

	@Override
	public String type() {
		return "line";
	}

	@Override
	public int area() {
		return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	@Override
	public boolean contains(int x, int y) {

		if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
			class Match {
				boolean value = false;
			}
			Match match = new Match();
			PointCallback iterator = new Geometry.PointCallback() {
				@Override
				public void iter(int cx, int cy) {
					if (x == cx && y == cy) {
						match.value = true;
					}
				}
			};
			Geometry.callOnLine(new Vector2i(x1, y1), new Vector2i(x2, y2), iterator);
			return match.value;
		}
		return false;
	}

}
