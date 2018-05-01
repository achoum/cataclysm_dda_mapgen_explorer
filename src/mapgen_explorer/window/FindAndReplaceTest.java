
package mapgen_explorer.window;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

public class FindAndReplaceTest extends JDialog implements ActionListener {

	private final JPanel contentPanel = new JPanel();
	private RSyntaxTextArea textArea;
	private JTextField searchField;
	private JCheckBox regexCB;
	private JCheckBox matchCaseCB;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			FindAndReplaceTest dialog = new FindAndReplaceTest();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public FindAndReplaceTest() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JPanel cp = new JPanel(new BorderLayout());

			textArea = new RSyntaxTextArea(20, 60);
			textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			textArea.setCodeFoldingEnabled(true);
			RTextScrollPane sp = new RTextScrollPane(textArea);
			cp.add(sp);

			// Create a toolbar with searching options.
			JToolBar toolBar = new JToolBar();
			searchField = new JTextField(30);
			toolBar.add(searchField);
			final JButton nextButton = new JButton("Find Next");
			nextButton.setActionCommand("FindNext");
			nextButton.addActionListener(this);
			toolBar.add(nextButton);
			searchField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					nextButton.doClick(0);
				}
			});
			JButton prevButton = new JButton("Find Previous");
			prevButton.setActionCommand("FindPrev");
			prevButton.addActionListener(this);
			toolBar.add(prevButton);
			regexCB = new JCheckBox("Regex");
			toolBar.add(regexCB);
			matchCaseCB = new JCheckBox("Match Case");
			toolBar.add(matchCaseCB);
			cp.add(toolBar, BorderLayout.NORTH);

			setContentPane(cp);
			setTitle("Find and Replace Demo");
			pack();
			setLocationRelativeTo(null);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// "FindNext" => search forward, "FindPrev" => search backward
		String command = e.getActionCommand();
		boolean forward = "FindNext".equals(command);

		// Create an object defining our search parameters.
		SearchContext context = new SearchContext();
		String text = searchField.getText();
		if (text.length() == 0) {
			return;
		}
		context.setSearchFor(text);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(forward);
		context.setWholeWord(false);

		boolean found = SearchEngine.find(textArea, context).wasFound();
		if (!found) {
			JOptionPane.showMessageDialog(this, "Text not found");
		}

	}

}
