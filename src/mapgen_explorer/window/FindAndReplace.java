package mapgen_explorer.window;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

public class FindAndReplace extends JDialog {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			FindAndReplace dialog = new FindAndReplace();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public FindAndReplace() {
		setTitle("Find and Replace");
		setBounds(100, 100, 450, 300);
	}

}
