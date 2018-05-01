
package mapgen_explorer.resources_loader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mapgen_explorer.render.shape.AbstractShape;
import mapgen_explorer.utils.Logger;
import mapgen_explorer.utils.RenderFilter;
import mapgen_explorer.utils.RenderFilter.eLayer;

import java.util.ArrayList;
import java.util.HashMap;

// Symbol palette i.e. mapping between a character to a list of item names.
public class Palette {

	public static class Item {
		public RenderFilter.eLayer layer;
		public ArrayList<String> entries = new ArrayList<>();
		public int chance = 1;
	}

	public HashMap<Character, ArrayList<Item>> char_to_id = new HashMap<>();

	// Load the palette from the prefab definition. Load the palette templates if any.
	public void loadFromPrefab(String main_directory, JSONObject json_prefab) throws Exception {
		JSONObject json_object = (JSONObject) json_prefab.get("object");
		loadFromContentObject(json_object);
		JSONArray json_palettes = (JSONArray) json_object.get("palettes");
		if (json_palettes != null) {
			for (int input_palette_idx = 0; input_palette_idx < json_palettes
					.size(); input_palette_idx++) {
				String input_palette_name = (String) json_palettes.get(input_palette_idx);
				loadFromGlobalTemplate(input_palette_name);
			}
		}
	}

	// Add the content of a palette template (i.e. the palette defined in "data/json/mapgen_palettes") to this palette.
	void loadFromGlobalTemplate(String input_palette_name) {
		Palette palette_template = Resources.palette_templates.palettes.get(input_palette_name);
		if (palette_template == null) {
			Logger.consoleWarnings("Palette template \"" + input_palette_name + "\" no found.");
			return;
		}
		char_to_id.putAll(palette_template.char_to_id);
	}

	public void loadFromJsonContentArray(JSONArray array) throws Exception {
		for (int item_idx = 0; item_idx < array.size(); item_idx++) {
			JSONObject item = (JSONObject) array.get(item_idx);
			String type = (String) item.get("type");
			if (!type.equals("palette")) {
				continue;
			}
			loadFromContentObject(item);
		}
	}

	// Load the palette from the prefab definition.
	void loadFromContentObject(JSONObject json_object) throws Exception {

		for (eLayer layer : eLayer.values()) {
			if (layer.sources == null) {
				continue;
			}
			for (String source : layer.sources) {
				JSONObject casted_object = (JSONObject) json_object.get(source);
				if (casted_object != null) {
					loadFromPaletteArray(casted_object, layer, source);
				}
			}
		}

		JSONObject json_mapping = (JSONObject) json_object.get("mapping");
		if (json_mapping != null) {
			loadFromMappingObject(json_mapping);
		}

	}

	RenderFilter.eLayer sourceToLayer(String query_source) {
		for (eLayer layer : eLayer.values()) {
			if (layer.sources == null) {
				continue;
			}
			for (String source : layer.sources) {
				if (source.equals(query_source)) {
					return layer;
				}
			}
		}
		return RenderFilter.eLayer.ITEMS;
	}

	public static int extractChance(JSONObject item, AbstractShape shape) {
		Long chance = (Long) item.get("chance");
		if (chance != null) {
			if (shape != null)
				shape.label += " [chance: " + chance + "]";
			return (int) (long) chance;
		}
		if (item.containsKey("density")) {
			Object uncasted_density = item.get("density");
			float density = 1;
			if (uncasted_density instanceof Long) {
				density = (long) item.get("density");
			} else if (uncasted_density instanceof Double) {
				density = (float) (double) item.get("density");
			}
			if (shape != null)
				shape.label += " [density: " + density + "]";
			return (int) (1 / density);
		}
		if (item.containsKey("freq")) {
			int freq = (int) (long) item.get("freq");
			if (shape != null)
				shape.label += " [freq: " + freq + "]";
			return shape.area() / freq;
		}
		return 1;
	}

	void loadFromMappingObject(JSONObject json_mapping) throws Exception {
		for (Object character : json_mapping.keySet()) {
			char casted_character = ((String) character).charAt(0);

			ArrayList<Item> item_set = char_to_id.get(casted_character);
			if (item_set == null) {
				item_set = new ArrayList<Item>();
				char_to_id.put(casted_character, item_set);
			}

			JSONObject layer_symbol = (JSONObject) json_mapping.get(character);
			for (Object layer : layer_symbol.keySet()) {
				Object symbol = layer_symbol.get(layer);
				if (symbol instanceof String) {
					String casted_symbol = (String) symbol;
					if (casted_symbol == null) {
						throw new Exception("Cannot parse " + layer_symbol);
					}
					Item item = new Item();
					item.layer = sourceToLayer((String) layer);
					item.entries.add(casted_symbol);
					item_set.add(item);
				} else if (symbol instanceof JSONObject) {
					Item item = new Item();
					item.layer = sourceToLayer((String) layer);
					String real_symbol = (String) ((JSONObject) symbol).get(item.layer.key_id);
					item.entries.add(real_symbol);
					if (real_symbol == null) {
						throw new Exception("Cannot parse " + layer_symbol);
					}
					item.chance = extractChance((JSONObject) symbol, null);
					item_set.add(item);
				} else if (symbol instanceof JSONArray) {
					JSONArray array = (JSONArray) symbol;
					for (int i = 0; i < array.size(); i++) {
						JSONObject sub_item = (JSONObject) array.get(i);
						Item item = new Item();
						item.layer = sourceToLayer((String) layer);
						String real_symbol = (String) sub_item.get(item.layer.key_id);
						if (real_symbol == null) {
							throw new Exception("Cannot parse " + layer_symbol);
						}
						item.entries.add(real_symbol);
						item.chance = extractChance(sub_item, null);
						item_set.add(item);
					}

				}
			}
		}
	}

	// Load the actual content of the palette.
	void loadFromPaletteArray(JSONObject json_map, RenderFilter.eLayer layer,
			String last_hope_symbol) throws Exception {

		for (Object untyped_key : json_map.keySet()) {
			String key = (String) untyped_key;
			Object json_item = json_map.get(key);
			ArrayList<Item> item_set = char_to_id.get(key.charAt(0));
			if (item_set == null) {
				item_set = new ArrayList<Item>();
				char_to_id.put(key.charAt(0), item_set);
			}
			Item item = new Item();
			item.layer = layer;
			if (key.length() != 1) {
				throw new Exception("Invalid terrain symbol:" + key);
			}
			if (json_item instanceof String) {
				item.entries.add((String) json_item);
			} else if (json_item instanceof JSONArray) {
				JSONArray json_item_array = (JSONArray) json_item;
				for (int sub_item_idx = 0; sub_item_idx < json_item_array.size(); sub_item_idx++) {
					Object sub_item_object = json_item_array.get(sub_item_idx);
					if (sub_item_object instanceof String) {
						item.entries.add((String) sub_item_object);
					} else if (sub_item_object instanceof JSONObject) {
						JSONObject sub_item_object_casted = (JSONObject) sub_item_object;
						String sub_item = (String) sub_item_object_casted.get(layer.key_id);
						if (sub_item == null) {
							item.entries.add(last_hope_symbol);
						} else {
							item.entries.add(sub_item);
						}
						if (sub_item_object_casted.containsKey("chance") && sub_item_idx == 0) {
							item.chance = (int) (long) sub_item_object_casted.get("chance");
						}
					} else {
						throw new Exception("Unsupported type:" + sub_item_object);
					}
				}

			} else if (json_item instanceof JSONObject) {
				String sub_item = (String) ((JSONObject) json_item).get(layer.key_id);
				if (sub_item == null) {
					item.entries.add(last_hope_symbol);
				} else {
					item.entries.add(sub_item);
				}
				if (((JSONObject) json_item).containsKey("chance")) {
					item.chance = (int) (long) ((JSONObject) json_item).get("chance");
				}
			} else {
				throw new Exception("Unsupported type in palette template:" + json_item.toString()
						+ " for " + untyped_key.toString());
			}

			if (item.entries.isEmpty()) {
				item.entries.add(layer.label + " [U]");
			}
			item_set.add(item);
		}
	}

	public void importPalette(Palette other) {
		char_to_id.putAll(other.char_to_id);
	}

}
