
package cataclysm_dda_prefab_explorer.utils;

// Various file related utility functions.
public class Files {

	static public String getExtension(String path) {
		String extension = "";
		int split_idx = path.lastIndexOf('.');
		if (split_idx > 0) {
			extension = path.substring(split_idx + 1);
		}
		return extension;
	}
}
