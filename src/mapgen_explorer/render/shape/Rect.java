
package mapgen_explorer.render.shape;

public class Rect extends AbstractShape {
	public int x1, y1, x2, y2;

	@Override
	public String type() {
		return "rect";
	}

	@Override
	public int area() {
		return (x2 - x1) * (y2 - y1);
	}

	@Override
	public boolean contains(int x, int y) {
		return x >= x1 && x <= x2 && y >= y1 && y <= y2;
	}
}
