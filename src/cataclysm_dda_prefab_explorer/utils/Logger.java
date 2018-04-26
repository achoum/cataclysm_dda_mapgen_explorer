
package cataclysm_dda_prefab_explorer.utils;

import javax.swing.*;

public class Logger {

	public static void fatal(String message) {
		System.err.println(message);
	}

	public static void fatal(Exception exc) {
		exc.printStackTrace();
		JOptionPane.showMessageDialog(null, exc.getMessage());
		System.exit(1);
	}

}
