
package mapgen_explorer.utils;

import java.awt.Color;

public class RenderFilter {

	public boolean[] show_layer = new boolean[eLayer.values().length];

	public enum eLayer {
		TERRAIN("terrain", Color.GRAY, "ter", "terrain"),
		BACKGROUND("terrain/background", Color.GRAY),
		FOREGROUND("terrain/foreground", Color.GRAY),
		FURNITURE("furniture", Color.ORANGE, "furn", "furniture"),
		TOILETS("toilets", Color.BLUE, "toi", "toilets"),
		ITEMS("items", Color.YELLOW, "item", new String[] { "items", "item" }),
		UNKNOWN("unknown symbol", Color.PINK),
		TRAP("trap", Color.YELLOW, "trap", "traps"),
		FIELD("field", Color.YELLOW, "field", "fields"),
		VENDING_MACHINE("vending machine", Color.YELLOW, "item_group", "vendingmachines"),
		NPC("npc", Color.GREEN, "class", "npcs"),
		MONSTER("monster", Color.RED, "monster", new String[] { "monster", "monsters" }),
		VEHICULE("vehicle", Color.CYAN, "vehicle", "vehicles"),

		SIGN("sign", Color.ORANGE, "NONE", "signs"),
		LIQUID("liquid", Color.ORANGE, "liquid", "liquids"),
		GASPUMP("gaspump", Color.ORANGE, "group", "gaspumps"),
		LOOT("loot", Color.ORANGE, "item", "loots")

		;
		public String label;
		public Color color;
		public String key_id;
		public String[] sources;

		eLayer(String label, Color color) {
			this.label = label;
			this.color = color;
		}

		eLayer(String label, Color color, String key_id, String source) {
			this.label = label;
			this.color = color;
			this.key_id = key_id;
			this.sources = new String[] { source };
		}

		eLayer(String label, Color color, String key_id) {
			this.label = label;
			this.color = color;
			this.key_id = key_id;
		}

		eLayer(String label, Color color, String key_id, String[] sources) {
			this.label = label;
			this.color = color;
			this.key_id = key_id;
			this.sources = sources;
		}

	}

	public static eLayer getSubTerrainLayer(eLayer layer, boolean foreground) {
		if (layer == eLayer.TERRAIN) {
			return foreground ? eLayer.FOREGROUND : eLayer.BACKGROUND;
		} else {
			return layer;
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
		if ((layer == eLayer.BACKGROUND || layer == eLayer.FOREGROUND)
				&& !isLayerVisible(eLayer.TERRAIN)) {
			return false;
		}
		return show_layer[layer.ordinal()];
	}

}
