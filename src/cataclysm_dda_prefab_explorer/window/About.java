
package cataclysm_dda_prefab_explorer.window;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// The credit window.
public class About extends JDialog {

	private final JPanel contentPanel = new JPanel();

	public About(JFrame parent) {
		super(parent);
		setType(Type.POPUP);
		setTitle("About");
		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModal(true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 428, 305);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane);
			{
				JTextArea txtrPrefabExplorerFor = new JTextArea();
				txtrPrefabExplorerFor.setEditable(false);
				txtrPrefabExplorerFor.setText(
						"Prefab Explorer for Cataclysm DDA\r\nVersion 0.1 (24 April 2018)\r\n------------------------------------\r\n\r\nBy Achoum (achoum@gmail.com)\r\nhttp://blog.mathieu.guillame-bert.com");
				scrollPane.setViewportView(txtrPrefabExplorerFor);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton closeButton = new JButton("Close");
				closeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				buttonPane.add(closeButton);
			}
		}
	}

}
