
package mapgen_explorer.window;

import javax.swing.*;
import java.awt.*;

// Small window with a progress bar. Used to show the progress during the content indexing.
public class Loading extends JDialog {
	private JProgressBar progress_ui;
	private JLabel label_ui;
	int progress = -1;
	int total_progress = 100;
	String label = "";

	public void update(int new_progress, int new_total_progress, String new_label) {
		if (total_progress != new_total_progress) {
			total_progress = new_total_progress;
			progress_ui.setValue(0);
			progress_ui.setMaximum(total_progress);
		}
		if (new_progress != progress) {
			progress = new_progress;
			progress_ui.setValue(progress);
		}
		if (!label.equals(new_label)) {
			label = new_label;
			label_ui.setText(label);
		}
	}

	public Loading(String title) {
		setTitle(title);
		setType(Type.POPUP);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModal(true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setAlwaysOnTop(true);
		setBounds(100, 100, 488, 81);
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(Editor.class.getResource("/mapgen_explorer/resources/prefab.png")));

		{
			progress_ui = new JProgressBar();
			progress_ui.setStringPainted(true);
			getContentPane().add(progress_ui, BorderLayout.CENTER);
		}

		label_ui = new JLabel("");
		label_ui.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(label_ui, BorderLayout.SOUTH);
		setLocationRelativeTo(null);
	}

}
