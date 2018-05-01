
package mapgen_explorer.render.shape;

import mapgen_explorer.utils.RenderFilter;

public abstract class AbstractShape {
	public RenderFilter.eLayer layer;
	public String label;

	abstract public String type();

	@Override
	public String toString() {
		return label;
	}

	abstract public int area();

	public abstract  boolean contains(int x, int y) ;
}
