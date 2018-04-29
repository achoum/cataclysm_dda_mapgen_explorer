
package mapgen_explorer.resources_loader;

import mapgen_explorer.utils.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

// Index the content of the palette templates i.e. data/json/mapgen_palettes.
public class PaletteTemplates {

	HashMap<String, Palette> palettes = new HashMap<>();

	public void load(String directory) throws Exception {
		File[] palette_files = (new File(directory)).listFiles();
		for (File palette_file : palette_files) {
			if (!FileUtils.getExtension(palette_file.getName()).equals("json")) {
				continue;
			}
			JSONParser parser = new JSONParser();
			FileReader reader = new FileReader(palette_file);
			JSONArray content_array = (JSONArray) parser.parse(reader);
			for (int item_idx = 0; item_idx < content_array.size(); item_idx++) {
				Object item = content_array.get(item_idx);
				if (!(item instanceof JSONObject))
					continue;
				JSONObject item_json = (JSONObject) item;
				if (!("palette".equals(item_json.get("type")))) {
					continue;
				}
				String id = (String) item_json.get("id");
				Palette palette = new Palette();
				palette.loadFromContentObject(item_json);
				palettes.put(id, palette);
			}
			reader.close();
		}
	}

	public void clear() {
		palettes.clear();
	}

	public void addNewTemplate(String id, JSONObject item_json) {
		try {
			Palette palette = new Palette();
			palette.loadFromContentObject(item_json);
			palettes.put(id, palette);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
