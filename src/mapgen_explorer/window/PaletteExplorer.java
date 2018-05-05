
package mapgen_explorer.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.resources_loader.Palette;
import mapgen_explorer.resources_loader.Palette.Item;
import mapgen_explorer.resources_loader.PaletteTemplates;
import mapgen_explorer.resources_loader.Resources;
import mapgen_explorer.resources_loader.Tiles.Tile;
import mapgen_explorer.utils.RenderFilter.eLayer;

import java.awt.Toolkit;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.JToolBar;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.InputMethodEvent;

public class PaletteExplorer extends JFrame {

	private JPanel contentPane;
	Palette palette;
	private JTable palette_table;
	TableRowSorter<TableModel> sorter;
	private JToolBar toolBar;
	private JCheckBox chckbxShowTilesWithout;
	private JTextField filter;
	private JButton button;
	TableModel table_model;
	Editor editor;

	String[] columnNames = new String[] { "FG", "GB", "Symbol", "Char", "Palette", "Layer",
			"Num. of use" };
	static int SYMBOL_COL_IDX = 2;
	static int CHARACTER_COL_IDX = 3;
	private JCheckBox hide_terrain_with_fournitures;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					String main_directory = "D:/games/Cataclysm/cataclysmdda-0.C-7328";
					JSONParser parser = new JSONParser();

					ContentIndex.Prefab prefab = new ContentIndex.Prefab(new File(
							"D:/games/Cataclysm/Cataclysm-DDA/data/json/mapgen/bike_shop.json"), 0,
							"bandit_cabin");
					Resources.static_init(main_directory);

					FileReader reader = new FileReader(prefab.file);
					Object content = parser.parse(reader);
					reader.close();

					if (!(content instanceof JSONArray))
						throw new Exception("Not a prefab");
					JSONArray content_array = (JSONArray) content;
					Object item = content_array.get(prefab.index);
					if (!(item instanceof JSONObject))
						throw new Exception("Not a prefab");
					JSONObject json_prefab = (JSONObject) item;

					Palette palette = new Palette();
					palette.loadFromPrefab(main_directory, json_prefab, content_array);

					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					Resources.static_init(main_directory);

					PaletteExplorer frame = new PaletteExplorer();
					frame.setPalette(palette);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	class TableModel extends AbstractTableModel {
		private String[] columnNames = null;
		private ArrayList<Object[]> data = null;

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.size();
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {
			return data.get(row)[col];
		}

		public Class getColumnClass(int c) {
			if (c == 0 || c == 1) {
				return Icon.class;
			} else {
				return getValueAt(0, c).getClass();
			}
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public void setValueAt(Object value, int row, int col) {
			data.get(row)[col] = value;
		}
	}

	public PaletteExplorer() {
		setTitle("Palette Explorer");
		setIconImage(Toolkit.getDefaultToolkit().getImage(
				PaletteExplorer.class.getResource("/mapgen_explorer/resources/palette.png")));
		setBounds(100, 100, 624, 735);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JScrollPane palette_table_scroll = new JScrollPane();
		contentPane.add(palette_table_scroll, BorderLayout.CENTER);

		palette_table = new JTable();
		palette_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (editor != null) {
					Character selected_char = getSeletedCharacter();
					if (selected_char != null) {
						editor.selected_character.setText(selected_char.toString());
					}
				}
			}
		});
		palette_table.setGridColor(Color.LIGHT_GRAY);
		palette_table.setRowHeight(24);
		palette_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		palette_table.setFillsViewportHeight(true);
		palette_table.setAutoCreateRowSorter(true);
		palette_table_scroll.setViewportView(palette_table);

		toolBar = new JToolBar();
		contentPane.add(toolBar, BorderLayout.NORTH);

		filter = new JTextField() {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (getText().isEmpty()) {
					g.setColor(new Color(150, 150, 150));
					g.drawString("Regex filter on symbol, layer, palette, character...", 10,
							g.getFontMetrics().getHeight());
				}
			}
		};
		filter.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				changeFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				changeFilter();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				changeFilter();
			}
		});

		button = new JButton("");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshTable();
			}
		});
		button.setIcon(new ImageIcon(
				PaletteExplorer.class.getResource("/mapgen_explorer/resources/refresh.png")));
		toolBar.add(button);
		toolBar.add(filter);
		filter.setColumns(10);

		chckbxShowTilesWithout = new JCheckBox("Only show defined in prefab");
		chckbxShowTilesWithout.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				refreshTable();
			}
		});

		hide_terrain_with_fournitures = new JCheckBox("Hide terrains with furnitures");
		hide_terrain_with_fournitures.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshTable();
			}
		});
		hide_terrain_with_fournitures.setSelected(true);
		toolBar.add(hide_terrain_with_fournitures);
		chckbxShowTilesWithout.setSelected(true);
		toolBar.add(chckbxShowTilesWithout);
		refreshTable();
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
		refreshTable();
	}

	void changeFilter() {
		RowFilter<TableModel, Object> rf = null;
		try {
			rf = RowFilter.regexFilter(filter.getText());
		} catch (java.util.regex.PatternSyntaxException e) {
			return;
		}
		sorter.setRowFilter(rf);

	}

	void refreshTable() {
		if (palette == null) {
			return;
		}

		// Build the index char -> number of use.
		HashMap<Character, Integer> number_of_use = new HashMap<>();
		if (editor != null) {
			number_of_use = editor.prefab_preview.rendering.getCharacterUsageMap();
		}

		// Build index "symbol -> character + layer" for the palettes.
		class CharacterAndLayer {
			public char character;
			public eLayer layer;
			public String palette;

			public CharacterAndLayer(char character, eLayer layer, String palette) {
				this.character = character;
				this.layer = layer;
				this.palette = palette;
			}
		}
		HashMap<String, ArrayList<CharacterAndLayer>> symbol_to_character = new HashMap<>();
		for (Entry<Character, ArrayList<Item>> symbol_set_1 : palette.char_to_id.entrySet()) {
			char character = symbol_set_1.getKey();

			boolean has_non_terrains = false;
			if (hide_terrain_with_fournitures.isSelected()) {
				for (Item symbol_set_2 : symbol_set_1.getValue()) {
					if (symbol_set_2.layer != eLayer.TERRAIN) {
						has_non_terrains = true;
						break;
					}
				}
			}

			for (Item symbol_set_2 : symbol_set_1.getValue()) {
				if (symbol_set_2.layer == eLayer.TERRAIN && has_non_terrains) {
					continue;
				}

				for (String symbol : symbol_set_2.entries) {
					ArrayList<CharacterAndLayer> item_set = symbol_to_character.get(symbol);
					if (item_set == null) {
						item_set = new ArrayList<>();
						symbol_to_character.put(symbol, item_set);
					}
					item_set.add(new CharacterAndLayer(character, symbol_set_2.layer, "local"));
				}
			}
		}

		ArrayList<Object[]> data = new ArrayList<Object[]>();
		for (Entry<String, Tile> tile : Resources.tiles.tiles.entrySet()) {
			String symbol = tile.getKey();
			ArrayList<CharacterAndLayer> characters_and_layer = symbol_to_character.get(symbol);
			boolean is_fill_ter = symbol.equals(palette.fill_ter);
			if (characters_and_layer == null) {
				if (!chckbxShowTilesWithout.isSelected() || is_fill_ter) {
					Object[] row = new Object[] { tile.getValue().getIcon(true),
							tile.getValue().getIcon(false), symbol, is_fill_ter ? "fill_ter" : "",
							"", "", 0 };
					data.add(row);
				}
			} else {
				for (CharacterAndLayer character_and_layer : characters_and_layer) {
					Object[] row = new Object[] { tile.getValue().getIcon(true),
							tile.getValue().getIcon(false), symbol,
							Character.toString(character_and_layer.character),
							character_and_layer.palette,
							character_and_layer.layer.name().toLowerCase(),
							number_of_use.getOrDefault(character_and_layer.character, 0) };
					data.add(row);
				}
			}
		}
		table_model = new TableModel();
		table_model.data = data;
		table_model.columnNames = columnNames;

		palette_table.setModel(table_model);
		sorter = new TableRowSorter<TableModel>(table_model);
		palette_table.setRowSorter(sorter);

		palette_table.getColumnModel().getColumn(0).setPreferredWidth(32);
		palette_table.getColumnModel().getColumn(1).setPreferredWidth(32);
		setTitle("Palette Explorer (" + data.size() + " result(s))");
		palette_table.getSelectionModel().setSelectionInterval(0, 0);
		changeFilter();
	}

	public Character getSeletedCharacter() {
		int row_idx = palette_table.getSelectedRow();
		if (row_idx == -1) {
			return null;
		}
		String character = (String) palette_table.getValueAt(row_idx, CHARACTER_COL_IDX);
		if (character.length() != 1) {
			return null;
		}
		return character.charAt(0);
	}

	public void setSelectedCharacter(char selected_character) {
		for (int row_idx = 0; row_idx < table_model.getRowCount(); row_idx++) {
			String character = (String) palette_table.getValueAt(row_idx, CHARACTER_COL_IDX);
			if (character.length() != 1) {
				continue;
			}
			char character_char = character.charAt(0);
			if (character_char == selected_character) {
				palette_table.setRowSelectionInterval(row_idx, row_idx);
				Rectangle rect = palette_table.getCellRect(row_idx, 0, true);
				palette_table.scrollRectToVisible(rect);
			}
		}
	}

	public void setEditor(Editor editor) {
		this.editor = editor;
	}

	public void setPalette(PaletteTemplates palette_templates) {
		palette = new Palette();
		for (Entry<String, Palette> other : palette_templates.palettes.entrySet()) {
			palette.mergePalette(other.getValue());
		}
		setPalette(palette);
		chckbxShowTilesWithout.setSelected(false);
	}

}
