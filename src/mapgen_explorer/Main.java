
package mapgen_explorer;

import mapgen_explorer.resources_loader.Resources;
import mapgen_explorer.utils.Logger;
import mapgen_explorer.window.MapgenExplorer;

import javax.swing.*;
import java.io.File;

public class Main {

	public static File selectWorkingDirectory() {
		JFileChooser chooser = new JFileChooser();
		String last_main_directory = Config.preferences.get("main_directory",
				(new java.io.File(".")).getAbsolutePath());
		chooser.setCurrentDirectory(new File(last_main_directory));
		chooser.setDialogTitle("Select the Cataclysm DDA directory.");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			Config.preferences.put("main_directory", chooser.getSelectedFile().getAbsolutePath());
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			File main_directory = selectWorkingDirectory();
			if (main_directory == null)
				return;
			Resources.static_init(main_directory.getAbsolutePath());
			MapgenExplorer dialog = new MapgenExplorer(main_directory.getAbsolutePath());
			dialog.setVisible(true);
		} catch (Exception e) {
			Logger.fatal(e);
		}
	}

}
