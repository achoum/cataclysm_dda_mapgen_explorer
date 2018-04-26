
package cataclysm_dda_prefab_explorer;

import cataclysm_dda_prefab_explorer.resources_loader.Resources;
import cataclysm_dda_prefab_explorer.utils.Logger;
import cataclysm_dda_prefab_explorer.window.PreviewExplorer;

import javax.swing.*;
import java.io.File;

public class Main {

	public static File selectWorkingDirectory() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("Select the Cataclysm DDA directory.");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
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
			PreviewExplorer dialog = new PreviewExplorer(main_directory.getAbsolutePath());
			dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			Logger.fatal(e);
		}
	}

}
