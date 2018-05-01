
package mapgen_explorer.window;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import mapgen_explorer.Config;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// The credit window.
public class About extends JDialog {

    public About(JFrame parent) {
        super(parent);
        setType(Type.POPUP);
        setTitle("About");
        setResizable(false);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 567, 320);
        getContentPane().setLayout(new BorderLayout());
        		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(Editor.class.getResource("/mapgen_explorer/resources/prefab.png")));
        JPanel contentPanel = new JPanel();
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
                        "Mapgen Explorer for Cataclysm DDA\r\nVersion " + Config.localVersion() + " (" + Config.release_date + ")\r\n------------------------------------\r\n\r\nhttps://github.com/achoum/cataclysm_dda_mapgen_explorer\r\n\r\nBy Achoum (achoum@gmail.com)\r\nhttp://blog.mathieu.guillame-bert.com");
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
