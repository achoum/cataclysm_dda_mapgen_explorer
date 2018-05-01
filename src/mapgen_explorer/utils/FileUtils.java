
package mapgen_explorer.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

// Various file related utility functions.
public class FileUtils {

	// Get the extension of a file. Example. "toto/tata.titi" -> "titi".
	static public String getExtension(String path) {
		String extension = "";
		int split_idx = path.lastIndexOf('.');
		if (split_idx > 0) {
			extension = path.substring(split_idx + 1);
		}
		return extension;
	}

	// Import a string from a file.
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static String readFile(File file, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(file.toPath());
		return new String(encoded, encoding);
	}

	// Export a string into a file.
	public static void writeFile(String path, Charset encoding, String content) throws IOException {
		Files.write(Paths.get(path), content.getBytes(encoding));
	}

}
