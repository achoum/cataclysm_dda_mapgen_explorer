
package mapgen_explorer.window;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FileDialog;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import mapgen_explorer.Config;
import mapgen_explorer.component.CheckBoxList;
import mapgen_explorer.component.PrefabRenderPanel;
import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.render.PrefabRendering;
import mapgen_explorer.resources_loader.Palette.Cell;
import mapgen_explorer.resources_loader.Resources;
import mapgen_explorer.resources_loader.Tiles;
import mapgen_explorer.utils.FileUtils;
import mapgen_explorer.utils.Logger;
import mapgen_explorer.utils.RenderFilter;
import mapgen_explorer.utils.Vector2i;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.JCheckBox;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ItemEvent;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import javax.swing.ImageIcon;

public class Editor extends JFrame implements WindowListener, PrefabRenderPanel.ActionCallBack {

	JPanel contentPane;
	// Non-saved edits.
	boolean has_pending_edits = false;
	String main_directory;
	PrefabRenderPanel prefab_preview;
	RSyntaxTextArea raw_json_editbox;
	ContentIndex.Prefab prefab_index;
	// Current selected tool.
	eTool tool = eTool.SET;
	JToggleButton tool_enable_set;
	JToggleButton tool_enable_get;
	boolean interface_loaded = false;
	JTextArea console_editbox;
	JMenu mnTileset;
	JCheckBoxMenuItem menu_checkbox_show_grid;
	boolean about_to_be_updated = false;
	// Timer used to trigger the refresh of the prefab rendering after the raw json editbox is modified.
	Timer to_refresh_timer;
	private JComboBox selected_terrain;
	private JComboBox selected_prefab;
    boolean prefab_selector_is_beeing_updated = false;
	boolean raw_json_is_being_updated = false;
	// If true, the user has agreed that the edit tools might modify the json formatting.
	boolean has_accepted_alert = false;

	enum eTool {
		SET, // Apply a character on the map.
		GET // Get a character from the map. 
	}

	static class CharAndSymbol {
		public char character;
		public String symbol;

		public CharAndSymbol(char character, String symbol) {
			this.character = character;
			this.symbol = symbol;
		}

		@Override
		public String toString() {
			return character + " : " + symbol;
		}
	}

	// For debug purpose only.
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					String main_directory = "D:/games/Cataclysm/cataclysmdda-0.C-7328";
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					Resources.static_init("D:/games/Cataclysm/cataclysmdda-0.C-7328");
					ContentIndex.Prefab to_open = new ContentIndex.Prefab(new File(
							"D:/games/Cataclysm/cataclysmdda-0.C-7328/data/json/mapgen/apartment_con.json"),
							0, "bandit_cabin");
					Editor frame = new Editor(main_directory);
					frame.setVisible(true);
					frame.open(to_open);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Editor(String main_directory) {
		this.main_directory = main_directory;
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(Editor.class.getResource("/mapgen_explorer/resources/prefab.png")));
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 1092, 673);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem menu_save = new JMenuItem("Save");
		menu_save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuSave();
			}
		});
		mnFile.add(menu_save);

		JMenuItem menu_save_as = new JMenuItem("Save As...");
		menu_save_as.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuSaveAs();
			}
		});
		mnFile.add(menu_save_as);

		JMenuItem menu_save_copy_as = new JMenuItem("Save a Copy As...");
		menu_save_copy_as.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuSaveCopyAs();
			}
		});
		mnFile.add(menu_save_copy_as);

		JSeparator separator = new JSeparator();
		mnFile.add(separator);

		JMenuItem menu_exit = new JMenuItem("Exit");
		menu_exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuExit();
			}
		});
		mnFile.add(menu_exit);

		JMenu edit_undo = new JMenu("Edit");
		menuBar.add(edit_undo);

		JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setEnabled(false);
		mntmUndo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuUndo();
			}
		});
		edit_undo.add(mntmUndo);

		JMenuItem edit_find = new JMenuItem("Find");
		edit_find.setEnabled(false);
		edit_find.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuFind();
			}
		});
		edit_undo.add(edit_find);

		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);

		JMenuItem edit_reset_zoom = new JMenuItem("Reset Zoom");
		edit_reset_zoom.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuResetZoom();
			}
		});
		mnView.add(edit_reset_zoom);

		JMenuItem edit_show_stats = new JMenuItem("Show Statistics");
		edit_show_stats.setEnabled(false);
		edit_show_stats.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuShowStats();
			}
		});
		mnView.add(edit_show_stats);

		JSeparator separator_1 = new JSeparator();
		mnView.add(separator_1);

		mnTileset = new JMenu("Tileset");
		mnView.add(mnTileset);

		menu_checkbox_show_grid = new JCheckBoxMenuItem("Show grid");
		menu_checkbox_show_grid.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				actionMenuChangeShowGrid();
			}
		});
		mnView.add(menu_checkbox_show_grid);

		JMenu menu_theme = new JMenu("Theme");
		mnView.add(menu_theme);

		JMenuItem menu_theme_dark = new JMenuItem("Dark");
		menu_theme_dark.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuDarkTheme();
			}
		});
		menu_theme.add(menu_theme_dark);

		JMenuItem menu_theme_light = new JMenuItem("Light");
		menu_theme_light.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMenuLightTheme();
			}
		});
		menu_theme.add(menu_theme_light);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem help_about = new JMenuItem("About");
		help_about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				About about = new About(Editor.this);
				about.setLocationRelativeTo(Editor.this);
				about.setVisible(true);
			}
		});
		mnHelp.add(help_about);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.3);
		contentPane.add(splitPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		splitPane.setRightComponent(panel);
		panel.setLayout(new BorderLayout(0, 0));

		prefab_preview = new PrefabRenderPanel();
		prefab_preview.setActionCallBack(this);
		panel.add(prefab_preview, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);
		panel_1.setLayout(new MigLayout("", "[][]", "[][][][][][][]"));

		JLabel lblNewLabel = new JLabel("Tools");
		panel_1.add(lblNewLabel, "flowy,cell 0 0");

		tool_enable_set = new JToggleButton("");
		tool_enable_set.setIcon(
				new ImageIcon(Editor.class.getResource("/mapgen_explorer/resources/edit.png")));
		panel_1.add(tool_enable_set, "cell 0 1");
		tool_enable_set.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setTool(eTool.SET, false);
			}
		});
		tool_enable_set.setSelected(true);

		tool_enable_get = new JToggleButton("");
		tool_enable_get.setIcon(
				new ImageIcon(Editor.class.getResource("/mapgen_explorer/resources/pipette.png")));
		panel_1.add(tool_enable_get, "cell 1 1");

		selected_terrain = new JComboBox();
		panel_1.add(selected_terrain, "cell 0 2 2 1,growx");

		JSeparator separator_2 = new JSeparator();
		panel_1.add(separator_2, "cell 0 3 2 1,growx");

		JLabel lblDisplay = new JLabel("Display");
		panel_1.add(lblDisplay, "cell 0 4");

		selected_prefab = new JComboBox();
		selected_prefab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (prefab_selector_is_beeing_updated) {
					return;
				}
				ContentIndex.PrefabWithoutFile selected_prefab_value = (ContentIndex.PrefabWithoutFile) selected_prefab
						.getSelectedItem();
				if (selected_prefab_value != null) {
					prefab_index.index = selected_prefab_value.index;
					refreshDisplayFromEditbox();
				}
			}
		});
		panel_1.add(selected_prefab, "cell 0 5 2 1,growx");

        CheckBoxList show_layers = new CheckBoxList();
		show_layers.setForeground(Color.BLACK);
		show_layers.setBorder(new LineBorder(Color.GRAY));
		panel_1.add(show_layers, "cell 0 6 2 1,growx");
		for (RenderFilter.eLayer layer : RenderFilter.eLayer.values()) {
			JCheckBox layer_checkbox = new JCheckBox(layer.label);
			layer_checkbox.setSelected(true);
			layer_checkbox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					prefab_preview.rendering.render_filter.setLayerVisible(layer,
							layer_checkbox.isSelected());
					try {
						prefab_preview.rendering.update();
					} catch (Exception e1) {
						// Okay not do to anything here.
					}
					prefab_preview.askUpdateRenderAndRedraw();
				}
			});
			show_layers.addCheckbox(layer_checkbox);
		}
		tool_enable_get.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setTool(eTool.GET, false);
			}
		});

		JPanel editor_console_panel = new JPanel();
		splitPane.setLeftComponent(editor_console_panel);
		editor_console_panel.setLayout(new BorderLayout(0, 0));

		JSplitPane editor_console_split = new JSplitPane();
		editor_console_split.setResizeWeight(0.8);
		editor_console_split.setOrientation(JSplitPane.VERTICAL_SPLIT);
		editor_console_panel.add(editor_console_split, BorderLayout.CENTER);

		JScrollPane console_scroll = new JScrollPane();
		editor_console_split.setRightComponent(console_scroll);

		console_editbox = new JTextArea();
		console_editbox.setEditable(false);
		console_editbox.setForeground(new Color(255, 255, 255));
		console_editbox.setBackground(Color.decode("#293134"));
		console_scroll.setViewportView(console_editbox);

		JPanel raw_json_edit_subpanel = new JPanel();
		editor_console_split.setLeftComponent(raw_json_edit_subpanel);
		raw_json_edit_subpanel.setLayout(new BorderLayout(0, 0));

		RTextScrollPane raw_json_editscroll = new RTextScrollPane();
		raw_json_edit_subpanel.add(raw_json_editscroll);
		raw_json_editscroll.setLineNumbersEnabled(true);
		raw_json_editscroll.setFoldIndicatorEnabled(true);

		raw_json_editbox = new RSyntaxTextArea();
		raw_json_editbox.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				contentHaveChanged();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				contentHaveChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				contentHaveChanged();
			}
		});

		to_refresh_timer = new Timer(500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshDisplayFromEditbox();
			}
		});
		to_refresh_timer.setRepeats(false);

		raw_json_editscroll.setViewportView(raw_json_editbox);
		raw_json_editbox.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
		raw_json_editbox.setCodeFoldingEnabled(true);
		raw_json_editbox.setLineWrap(true);

		changeTheme(Config.default_editor_theme);
		interface_loaded = true;
		listTilesets();
		addWindowListener(this);
	}

	// Update the title of the window.
	void updateTitle() {
		if (prefab_index == null) {
			return;
		}
		setTitle("Prefab Editor - " + (has_pending_edits ? "*" : "")
				+ prefab_index.file.getAbsolutePath());
	}

	// Change the theme of the raw json editor.
	void changeTheme(String name) {
		try {
			Theme theme = Theme.load(getClass()
					.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + name + ".xml"));
			theme.apply(raw_json_editbox);
		} catch (IOException e) {
			Logger.fatal(e);
		}
	}

	// Open a prefab.
	protected void open(ContentIndex.Prefab prefab_index) throws Exception {
		this.prefab_index = prefab_index;
		// Load the json in the edit text box.
		String raw_prefab = FileUtils.readFile(prefab_index.file.getAbsolutePath(),
				StandardCharsets.UTF_8);
		raw_json_editbox.setText(raw_prefab);
		raw_json_editbox.setCaretPosition(0);
		has_pending_edits = false;
		refreshDisplayFromEditbox();
		prefab_preview.rendering.setShowGrid(menu_checkbox_show_grid.isSelected());
		prefab_preview.askRedraw();
		updateTitle();
	}

	// Update the prefab render from the content of the editbox.
	public void refreshDisplayFromEditbox() {
		try {
			prefab_preview.loadPrefab(main_directory, prefab_index, raw_json_editbox.getText());
			console_editbox.setText("Json parsing successful");
			refreshSelectedTerrain();
			refreshSelectedPrefab();
		} catch (Exception e) {
			console_editbox.setText("Json parsing failed:\n" + e.toString());
		}
	}

	// Refresh the list of available terrains from the content of the palette of the prefab.
	// It a terrain item was selected, the same terrain item will remain selected (if it still exists).
	void refreshSelectedTerrain() {
		CharAndSymbol save_selected_terrain_value = (CharAndSymbol) selected_terrain
				.getSelectedItem();
		selected_terrain.removeAllItems();
		CharAndSymbol new_selected_terrain_value = null;
		for (Entry<Character, Cell> terrain : prefab_preview.palette.char_to_id.entrySet()) {
			ArrayList<String> symbols = terrain.getValue().entries;
			String all_symbols;
			if (symbols.isEmpty()) {
				all_symbols = "<none>";
			} else if (symbols.size() == 1) {
				all_symbols = symbols.get(0);
			} else {
				all_symbols = symbols.get(0) + " + ...";
			}
			CharAndSymbol new_entry = new CharAndSymbol(terrain.getKey(), all_symbols);
			if (save_selected_terrain_value != null
					&& new_entry.character == save_selected_terrain_value.character) {
				new_selected_terrain_value = new_entry;
			}
			selected_terrain.addItem(new_entry);
		}
		if (new_selected_terrain_value != null) {
			selected_terrain.setSelectedItem(new_selected_terrain_value);
		}
	}

	// Refresh the list of available prefab in the json file.
	// It a prefab item was selected, the same prefab item will remain selected (if it still exists).
	void refreshSelectedPrefab() {
		prefab_selector_is_beeing_updated = true;
		selected_prefab.removeAllItems();
		ContentIndex.PrefabWithoutFile new_selected_prefab_value = null;
		for (ContentIndex.PrefabWithoutFile prefab : ContentIndex
				.listPrefabInJson(raw_json_editbox.getText())) {
			if (prefab.index == prefab_index.index) {
				new_selected_prefab_value = prefab;
			}
			selected_prefab.addItem(prefab);
		}
		if (new_selected_prefab_value != null) {
			selected_prefab.setSelectedItem(new_selected_prefab_value);
		}
		prefab_selector_is_beeing_updated = false;
	}

	// Get the button that correspond to a given tool.
	JToggleButton getToolButton(eTool tool) {
		switch (tool) {
		case GET:
			return tool_enable_get;
		case SET:
			return tool_enable_set;
		}
		return null;
	}

	// Change the active tool.
	protected void setTool(eTool new_tool, boolean enable_my_button) {
		if (interface_loaded) {
			JToggleButton old_button = getToolButton(tool);
			if (old_button.isSelected()) {
				old_button.setSelected(false);
			}
		}
		this.tool = new_tool;
		if (interface_loaded && enable_my_button) {
			JToggleButton new_button = getToolButton(tool);
			if (!new_button.isSelected()) {
				getToolButton(tool).setSelected(true);
			}
		}
	}

	// Call when the content of the raw json is changed. Triggered the refresh of the prefab rendering with some delays.
	protected void contentHaveChanged() {
		boolean save_has_pending_edits = has_pending_edits;
		has_pending_edits = true;
		if (!save_has_pending_edits) {
			updateTitle();
		}
		if (!to_refresh_timer.isRunning() && !raw_json_is_being_updated) {
			to_refresh_timer.start();
		}
	}

	// List the available tilesets.
	void listTilesets() {
		mnTileset.removeAll();
		ArrayList<String> tilesets = Tiles.listTilesets(main_directory);
		for (String tileset : tilesets) {
			JMenuItem tileset_menu_item = new JMenuItem(tileset);
			mnTileset.add(tileset_menu_item);
			tileset_menu_item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setTileset(tileset);
				}
			});
		}
	}

	// Change the active tileset.
	void setTileset(String tileset) {
		System.out.println("Change tileset to " + tileset);
		try {
			Resources.tiles.load(main_directory, tileset);
			refreshDisplayFromEditbox();
		} catch (Exception e) {
			Logger.fatal(e);
		}
	}

	// Change the display theme of the text editor.
	void actionMenuDarkTheme() {
		changeTheme("dark");
	}

	void actionMenuLightTheme() {
		changeTheme("default");
	}

	protected void actionMenuExit() {
		if (has_pending_edits) {
			int dialogResult = JOptionPane.showConfirmDialog(this,
					"Save to " + prefab_index.file.getAbsolutePath(), "Saving before existing",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION) {
				if (actionMenuSave()) {
					dispose();
				}
			} else if (dialogResult == JOptionPane.NO_OPTION) {
				dispose();
			} else if (dialogResult == JOptionPane.CANCEL_OPTION) {
				return;
			}
		} else {
			dispose();
		}
	}

	protected void actionMenuChangeShowGrid() {
		prefab_preview.rendering.setShowGrid(menu_checkbox_show_grid.isSelected());
		prefab_preview.askRedraw();
	}

	protected void actionMenuResetZoom() {
		prefab_preview.resetZoomToShowAll();
	}

	protected void actionMenuSaveCopyAs() {
		FileDialog fileDialogSave = new FileDialog(this, "Save As", FileDialog.SAVE);
		fileDialogSave.setVisible(true);
		if (fileDialogSave.getFile() == null) {
			// Cancel.
			return;
		}
		String save_path = fileDialogSave.getDirectory() + fileDialogSave.getFile();
		try {
			FileUtils.writeFile(save_path, StandardCharsets.UTF_8, raw_json_editbox.getText());
		} catch (IOException e) {
			Logger.fatal(e);
		}
	}

	protected void actionMenuSaveAs() {
		FileDialog fileDialogSave = new FileDialog(this, "Save As", FileDialog.SAVE);
		fileDialogSave.setVisible(true);
		if (fileDialogSave.getFile() == null) {
			// Cancel.
			return;
		}
		String save_path = fileDialogSave.getDirectory() + fileDialogSave.getFile();
		File save_file = new File(save_path);
		prefab_index.file = save_file;
		actionMenuSave();
		updateTitle();
	}

	protected boolean actionMenuSave() {
		try {
			FileUtils.writeFile(prefab_index.file.getAbsolutePath(), StandardCharsets.UTF_8,
					raw_json_editbox.getText());
			has_pending_edits = false;
			updateTitle();
		} catch (IOException e) {
			Logger.fatal(e);
		}
		return true;
	}

	protected void actionMenuShowStats() {
		// TODO Auto-generated method stub

	}

	protected void actionMenuUndo() {
		// TODO Auto-generated method stub

	}

	protected void actionMenuFind() {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		actionMenuExit();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	// Call back from clicking with the left mouse button on the prefab rendering.
	@Override
	public void action(Vector2i cell_coordinate) {
		switch (tool) {
		case GET: {
			PrefabRendering.Cell cell = prefab_preview.rendering.getCell(cell_coordinate);
			if (cell != null) {
				selectTerrainByCharacter(cell.character);
			}
		}
			break;
		case SET: {
			CharAndSymbol new_character_item = (CharAndSymbol) selected_terrain.getSelectedItem();
			if (new_character_item != null) {
				setCellCharacter(prefab_index.index, cell_coordinate, new_character_item.character);
			}
		}
			break;
		default:
			break;
		}
	}

	// Change the character of a cell in the map.
	private void setCellCharacter(int prefab_index, Vector2i cell_position, char character) {

		if (!has_accepted_alert) {
			int alert_answer = JOptionPane.showConfirmDialog(this,
					"Using this tool might change the formatting of the JSON file.\n"
							+ "Do you want to continue? If you press YES, we wont ask again.\n"
							+ "The file wont be modified until you save.",
					"Using Edition Tools", JOptionPane.YES_NO_OPTION);
			if (alert_answer == JOptionPane.YES_OPTION) {
				has_accepted_alert = true;
			} else {
				return;
			}
		}

		try {
			JSONParser parser = new JSONParser();
			Object content = parser.parse(raw_json_editbox.getText());
			if (!(content instanceof JSONArray))
				throw new Exception("Not a prefab");
			JSONArray content_array = (JSONArray) content;
			Object item = content_array.get(prefab_index);
			if (!(item instanceof JSONObject))
				throw new Exception("Not a prefab");
			JSONObject json_prefab = (JSONObject) item;
			JSONObject json_object = (JSONObject) json_prefab.get("object");
			JSONArray json_row = (JSONArray) json_object.get("rows");
			String row = (String) json_row.get(cell_position.y);
			String new_row = row.substring(0, cell_position.x) + character
					+ row.substring(cell_position.x + 1);
			json_row.set(cell_position.y, new_row);
			has_pending_edits = true;
			updateTitle();
			raw_json_is_being_updated = true;
			raw_json_editbox.setText(content_array.toJSONString());
			raw_json_is_being_updated = false;
			refreshDisplayFromEditbox();
		} catch (Exception e) {
			raw_json_is_being_updated = false;
			e.printStackTrace();
		}
	}

	// Change the selected terrain type according a character.
	private void selectTerrainByCharacter(char character) {
		for (int item_idx = 0; item_idx < selected_terrain.getItemCount(); item_idx++) {
			CharAndSymbol item = (CharAndSymbol) selected_terrain.getItemAt(item_idx);
			if (item.character == character) {
				selected_terrain.setSelectedIndex(item_idx);
				break;
			}
		}
	}

}
