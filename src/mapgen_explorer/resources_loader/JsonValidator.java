
package mapgen_explorer.resources_loader;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.render.PrefabRendering;
import mapgen_explorer.utils.Logger;

// Check the validity of the content of a json file.
public class JsonValidator {

	// Returns a error in HTML format.
	public static String checkError(String main_directory, File json_file,
			BufferedImage tmp_buffer) {
		Logger.enableSaveErrors();
		try {
			JSONParser parser = new JSONParser();
			FileReader reader = new FileReader(json_file);
			Object content = parser.parse(reader);
			reader.close();
			if (!(content instanceof JSONArray))
				throw new Exception("Not a prefab");
			JSONArray content_array = (JSONArray) content;

			Palette global_palette = new Palette();
			global_palette.loadFromJsonContentArray(content_array);

			for (int entry_idx = 0; entry_idx < content_array.size(); entry_idx++) {
				try {
					Object entry = content_array.get(entry_idx);
					if (!(entry instanceof JSONObject))
						continue;
					JSONObject json_entry = (JSONObject) entry;
					String type = (String) json_entry.get("type");
					if (type.equals("mapgen")) {
						checkErrorMapGen(main_directory, json_entry, global_palette, tmp_buffer);
					}

				} catch (Exception e) {
					return Logger.getClearAndDisableSavedErrors().replace("\n", "<br/>")
							+ e.toString() + "<br/>";
				}
			}

		} catch (Exception e) {
			return Logger.getClearAndDisableSavedErrors().replace("\n", "<br/>") + e.toString()
					+ "<br/>";
		}
		return Logger.getClearAndDisableSavedErrors().replace("\n", "<br/>");
	}

	static void checkErrorMapGen(String main_directory, JSONObject json_entry,
			Palette global_palette, BufferedImage tmp_buffer) throws Exception {
		if (!ContentIndex.fieldIsStringValue(json_entry, "method", "json"))
			return;
		Palette entry_palette = new Palette();
		entry_palette.loadFromPrefab(main_directory, json_entry);
		entry_palette.importPalette(global_palette);
		PrefabRendering rendering = new PrefabRendering(json_entry, entry_palette);
		rendering.update();
		rendering.render((Graphics2D) tmp_buffer.getGraphics(), 0, 0, tmp_buffer.getWidth(),
				tmp_buffer.getHeight());
	}

}
