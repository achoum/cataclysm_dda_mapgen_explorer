
package mapgen_explorer.resources_loader;

import mapgen_explorer.Config;

import javax.swing.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Random;

// List of static resources and assets. 
public class Resources {
	public static Icons icons;
	public static Tiles tiles;
	// The palette in the palette directory.
	public static PaletteTemplates global_palette_templates;
	// All the palette indexed from the refab.
	public static PaletteTemplates indexed_palette_templates;
	public static Random random = new Random();

	static ImageIcon getIcon(String filename) {
		String path = "mapgen_explorer/resources/" + filename;
		URL url = Resources.class.getClassLoader().getResource(path);
		System.out.println(path + " : " + url);
		return new ImageIcon(url);
	}

	public static void static_init(String main_directory) throws Exception {
		System.out.println("Loading resources from " + main_directory);
		icons = new Icons();
		icons.directory = getIcon("directory.png");
		icons.jsonfile = getIcon("json-file.png");
		icons.prefab = getIcon("prefab.png");
		icons.palette = getIcon("palette.png");
		icons.missing = getIcon("missing.png");
		tiles = new Tiles();
		tiles.load(main_directory, Config.default_tileset);
		global_palette_templates = new PaletteTemplates();
		indexed_palette_templates = new PaletteTemplates();
		global_palette_templates
				.load(Paths.get(main_directory, "data", "json", "mapgen_palettes").toString());
	}

}
