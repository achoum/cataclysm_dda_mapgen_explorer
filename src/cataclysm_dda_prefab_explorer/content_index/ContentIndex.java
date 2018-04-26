
package cataclysm_dda_prefab_explorer.content_index;

import cataclysm_dda_prefab_explorer.utils.Files;
import cataclysm_dda_prefab_explorer.utils.Logger;
import cataclysm_dda_prefab_explorer.window.Loading;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

// Index of the game content. Currently, only the prefabs are indexed.
public class ContentIndex {

	public Directory root = null;

	static class Progress {
		public int current = 0;
		public int total = 0;
	}

	public static class Directory {
		public ArrayList<Directory> sub_directories = new ArrayList<>();
		public ArrayList<JsonFile> json_files = new ArrayList<>();
		public File file;

		@Override
		public String toString() {
			return file.getName();
		}

		public Directory(File file) {
			this.file = file;
		}
	}

	public static class JsonFile {
		public ArrayList<Prefab> prefabs = new ArrayList<>();
		public File file;

		@Override
		public String toString() {
			return file.getName();
		}

		public JsonFile(File file) {
			this.file = file;
		}
	}

	public static class Prefab {
		public File file;
		public int index;
		public String base_name;

		public Prefab(File file, int index, String base_name) {
			this.file = file;
			this.index = index;
			this.base_name = base_name;
		}

		@Override
		public String toString() {
			return "#" + Integer.toString(index) + " : " + base_name;
		}

	}

	public void update(String main_directory) {
		Loading loading = new Loading("Indexing content");
		Thread progress_bar_thread = new Thread() {
			@Override
			public void run() {
				root = new Directory(new File(main_directory));
				loading.update(0, 100, "Indexing files");
				indexDirectory(root);
				Progress progress = new Progress();
				progress.total = getNumberOfJsonFiles(root);
				recursiveIndexJsonFiles(root, loading, progress);
				loading.dispose();
			}
		};
		progress_bar_thread.start();
		loading.setVisible(true);
	}

	void indexDirectory(Directory directory) {
		File[] files = directory.file.listFiles();
		if (files == null) {
			Logger.fatal(directory + " is not a directory");
		}
		for (File file : files) {
			if (file.isDirectory()) {
				Directory sub_directory = new Directory(file);
				directory.sub_directories.add(sub_directory);
				indexDirectory(sub_directory);
			} else {
				String extension = Files.getExtension(file.getName());
				if (extension.equals("json")) {
					JsonFile json_file = new JsonFile(file);
					directory.json_files.add(json_file);
				}
			}
		}
	}

	int getNumberOfJsonFiles(Directory directory) {
		int count = directory.json_files.size();
		for (Directory sub_directory : directory.sub_directories) {
			count += getNumberOfJsonFiles(sub_directory);
		}
		return count;
	}

	void recursiveIndexJsonFiles(Directory directory, Loading loading, Progress progress) {
		Iterator<JsonFile> json_file_iterator = directory.json_files.iterator();
		while (json_file_iterator.hasNext()) {
			JsonFile json_file = json_file_iterator.next();
			indexJsonFile(json_file);
			loading.update(++progress.current, progress.total,
					"Scanning " + json_file.file.getPath());
			if (json_file.prefabs.isEmpty()) {
				// Remove the json file without prefabs.
				json_file_iterator.remove();
			}
		}

		Iterator<Directory> sub_directory_iterator = directory.sub_directories.iterator();
		while (sub_directory_iterator.hasNext()) {
			Directory sub_directory = sub_directory_iterator.next();
			recursiveIndexJsonFiles(sub_directory, loading, progress);
			if (sub_directory.sub_directories.isEmpty() && sub_directory.json_files.isEmpty()) {
				sub_directory_iterator.remove();
			}
		}
	}

	void indexJsonFile(JsonFile json_file) {
		try {
			System.out.println("Scanning " + json_file.file.getCanonicalPath());
			JSONParser parser = new JSONParser();
			FileReader reader = new FileReader(json_file.file);
			Object content = parser.parse(reader);
			indexJsonContentArray(json_file, content);
			reader.close();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	boolean fieldIsStringValue(JSONObject json_object, String field_name, String expected_value) {
		Object type = json_object.getOrDefault(field_name, null);
		if (type == null || !(type instanceof String)) {
			return false;
		}
		return expected_value.equals(type);
	}

	void indexJsonContentArray(JsonFile json_file, Object content) {
		if (!(content instanceof JSONArray))
			return;
		JSONArray content_array = (JSONArray) content;
		for (int item_idx = 0; item_idx < content_array.size(); item_idx++) {
			Object item = content_array.get(item_idx);
			if (!(item instanceof JSONObject))
				continue;
			JSONObject item_json = (JSONObject) item;
			if (!fieldIsStringValue(item_json, "type", "mapgen"))
				continue;
			if (!fieldIsStringValue(item_json, "method", "json"))
				continue;
			Object json_om_terrain = item_json.get("om_terrain");
			if (json_om_terrain == null)
				continue;
			Prefab prefab = new Prefab(json_file.file, item_idx, json_om_terrain.toString());
			json_file.prefabs.add(prefab);
		}
	}

}
