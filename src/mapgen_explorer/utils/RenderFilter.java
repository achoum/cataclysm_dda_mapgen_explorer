
package mapgen_explorer.utils;

public class RenderFilter {

	public boolean[] show_layer = new boolean[eLayer.values().length];

	public enum eLayer {
		FILL_TER("fill_ter"),
		BACKGROUND("background"),
		FOREGROUND("foreground"),
		POINTS_AND_LINES("points and lines"),
		ITEMS("items"),
		MONSTERS("monsters");
		public String label;

		eLayer(String label) {
			this.label = label;
		}
	}

	public void setLayerVisible(eLayer layer, boolean visible) {
		show_layer[layer.ordinal()] = visible;

	}

	public void setAllVisible() {
		for (int layer_idx = 0; layer_idx < show_layer.length; layer_idx++) {
			show_layer[layer_idx] = true;
		}
	}

	public boolean isLayerVisible(eLayer layer) {
		return show_layer[layer.ordinal()];
	}

}
