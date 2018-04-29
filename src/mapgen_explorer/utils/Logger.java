
package mapgen_explorer.utils;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

public class Logger {

	static HashMap<String, Integer> error_set = null;
	static ArrayList<String> error_list = null;

	public static void enableSaveErrors() {
		error_set = new HashMap();
		error_list = new ArrayList();
	}

	public static String getClearAndDisableSavedErrors() {
		StringBuilder sb = new StringBuilder();
		for (String error : error_list) {
			sb.append("[x" + error_set.get(error) + "] ");
			sb.append(error);
			sb.append("\n");
		}
		error_set = null;
		error_list = null;
		return sb.toString();
	}

	public static void consoleWarnings(String message) {
		if (error_set != null) {
			if (!error_set.containsKey(message)) {
				error_set.put(message, 1);
				error_list.add(message);
				System.err.println(message);
			} else {
				error_set.put(message, error_set.get(message) + 1);
			}
		} else {
			System.err.println(message);
		}
	}

	public static void fatal(String message) {
		System.err.println(message);
		JOptionPane.showMessageDialog(null, message);
	}

	public static void fatal(Exception exc) {
		fatal(exc.getMessage());
	}

}
