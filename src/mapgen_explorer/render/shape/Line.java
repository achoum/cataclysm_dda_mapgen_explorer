
package mapgen_explorer.render.shape;

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

}
