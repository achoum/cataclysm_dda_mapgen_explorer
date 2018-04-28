
package mapgen_explorer.utils;

import javax.swing.*;

public class Logger {

	public static void fatal(String message) {
		System.err.println(message);
		JOptionPane.showMessageDialog(null, message);
		System.exit(1);
	}

	public static void fatal(Exception exc) {
		fatal(exc.getMessage());
	}

}
