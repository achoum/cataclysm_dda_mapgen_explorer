
package mapgen_explorer;

import java.util.prefs.Preferences;

public class Config {
	public final static String default_tileset = "ChestHoleTileset";
	public final static String version = "0.3";
	public static String release_date = "29 April 2018";
	public static String default_editor_theme = "dark";

	public static Preferences preferences = Preferences.userNodeForPackage(Config.class);
}
