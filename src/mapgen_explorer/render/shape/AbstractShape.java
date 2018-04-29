
package mapgen_explorer.render.shape;

import mapgen_explorer.utils.RenderFilter;

public abstract class AbstractShape {
	public RenderFilter.eLayer layer;
	public String label;
	public int chance = 100;
	public int repeat_min;
	public int repeat_max = -1;

	abstract public String type();

	@Override
	public String toString() {
		String name = label + " [" + type() + "]";
		if (chance != 100) {
			name += " chance:" + chance;
		}
		if (repeat_max != -1) {
			name += " repeat: [" + repeat_min + "," + repeat_max + "]";
		}
		return name;
	}

	abstract public int area();
}
