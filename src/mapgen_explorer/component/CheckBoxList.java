
package mapgen_explorer.component;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

// Inspired from "http://www.devx.com/tips/Tip/5342".
// List of checkboxes. Used to select the layers "to display".
public class CheckBoxList extends JList {
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public CheckBoxList() {
		setCellRenderer(new CellRenderer());

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
					checkbox.setSelected(!checkbox.isSelected());
					repaint();
				}
			}
		});

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public void addCheckbox(JCheckBox checkBox) {
		ListModel currentList = this.getModel();
		JCheckBox[] newList = new JCheckBox[currentList.getSize() + 1];
		for (int i = 0; i < currentList.getSize(); i++) {
			newList[i] = (JCheckBox) currentList.getElementAt(i);
		}
		newList[newList.length - 1] = checkBox;
		setListData(newList);
	}

	protected class CellRenderer implements ListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JCheckBox checkbox = (JCheckBox) value;
			checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
			checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
			checkbox.setEnabled(isEnabled());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
					: noFocusBorder);
			return checkbox;
		}
	}
}