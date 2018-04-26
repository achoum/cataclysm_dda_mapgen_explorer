
package cataclysm_dda_prefab_explorer.resources_loader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import cataclysm_dda_prefab_explorer.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;

// Symbol palette i.e. mapping between a character to a list of item names.
public class Palette {

	public static class Cell {
		public ArrayList<String> entries = new ArrayList<>();
	}

	public HashMap<Character, Cell> char_to_id = new HashMap<>();

	// Load the palette from the prefab definition. Load the palette templates if any.
	public void load(String main_directory, JSONObject json_prefab) throws Exception {
		JSONObject json_object = (JSONObject) json_prefab.get("object");
		loadPalette(json_object);
		JSONArray json_palettes = (JSONArray) json_object.get("palettes");
		if (json_palettes != null) {
			for (int input_palette_idx = 0; input_palette_idx < json_palettes
					.size(); input_palette_idx++) {
				String input_palette_name = (String) json_palettes.get(input_palette_idx);
				importPaletteTemplate(input_palette_name);
			}
		}
	}

	// Add the content of a palette template (i.e. the palette defined in "data/json/mapgen_palettes") to this palette.
	void importPaletteTemplate(String input_palette_name) {
		Palette palette_template = Resources.palette_templates.palettes.get(input_palette_name);
		if (palette_template == null) {
			System.err.println("Palette template \"" + palette_template + "\" no found.");
			return;
		}
		char_to_id.putAll(palette_template.char_to_id);
	}

	// Load the palette from the prefab definition.
	void loadPalette(JSONObject json_object) throws Exception {
		JSONObject json_terrain = (JSONObject) json_object.get("terrain");
		if (json_terrain != null) {
			loadPaletteMap(json_terrain);
		}
		JSONObject json_furniture = (JSONObject) json_object.get("furniture");
		if (json_furniture != null) {
			loadPaletteMap(json_furniture);
		}
	}

	// Load the actual content of the palette.
	void loadPaletteMap(JSONObject json_map) throws Exception {
		for (Object untyped_key : json_map.keySet()) {
			String key = (String) untyped_key;
			Object json_item = json_map.get(key);
			Cell item = new Cell();
			if (key.length() != 1) {
				throw new Exception("Invalid terrain symbol:" + key);
			}
			char_to_id.put(key.charAt(0), item);
			if (json_item instanceof String) {
				item.entries.add((String) json_item);
			} else if (json_item instanceof JSONArray) {
				JSONArray json_item_array = (JSONArray) json_item;
				for (int sub_item_idx = 0; sub_item_idx < json_item_array.size(); sub_item_idx++) {
					Object sub_item_object = json_item_array.get(sub_item_idx);
					String sub_item = null;
					if (sub_item_object instanceof String) {
						sub_item = (String) sub_item_object;
					} else if (sub_item_object instanceof JSONObject) {
						sub_item = (String) ((JSONObject) sub_item_object).get("ter");
					} else {
						Logger.fatal("Unsupported type:" + sub_item_object);
					}
					item.entries.add(sub_item);
				}
			} else {
				throw new Exception("Unsupported type in palette template:" + json_item.toString());
			}
		}
	}

}
