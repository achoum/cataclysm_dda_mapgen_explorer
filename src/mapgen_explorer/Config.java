
package mapgen_explorer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class Config {
	public final static String default_tileset = "ChestHoleTileset";
	public final static String unknown_local_version = "UNKNOWN";
	public static String release_date = "29 April 2018";
	public static String default_editor_theme = "dark";
	public static String remote_version_path = "https://raw.githubusercontent.com/achoum/cataclysm_dda_mapgen_explorer/master/src/mapgen_explorer/VERSION";
	public static String local_version_path = "mapgen_explorer/VERSION";
	public static Preferences preferences = Preferences.userNodeForPackage(Config.class);
	public static String download_webpage = "https://github.com/achoum/cataclysm_dda_mapgen_explorer/releases";

	private static String local_version_cache = null;

	public static String localVersion() {
		if (local_version_cache == null) {
			local_version_cache = unknown_local_version;
			BufferedInputStream input_stream = new BufferedInputStream(
					Config.class.getClassLoader().getResourceAsStream(local_version_path));
			Scanner s = new Scanner(input_stream).useDelimiter("\\A");
			local_version_cache = s.hasNext() ? s.next() : "";
			s.close();
		}
		return local_version_cache;
	}

	public static String remoteVersion() {
		try {
			URL url = new URL(remote_version_path);
			Scanner s = new Scanner(url.openStream()).useDelimiter("\\A");
			String remote_version = s.hasNext() ? s.next() : "";
			s.close();
			return remote_version;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
