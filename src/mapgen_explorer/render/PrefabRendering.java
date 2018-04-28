
package mapgen_explorer.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mapgen_explorer.resources_loader.Palette;
import mapgen_explorer.resources_loader.Resources;
import mapgen_explorer.resources_loader.Tiles;
import mapgen_explorer.resources_loader.Tiles.RenderTile;
import mapgen_explorer.utils.Geometry;
import mapgen_explorer.utils.Logger;
import mapgen_explorer.utils.RenderFilter;
import mapgen_explorer.utils.RenderFilter.eLayer;
import mapgen_explorer.utils.Vector2i;

// The project of a prefab object into something ready to be rendered.
public class PrefabRendering {

	// Size of the prefab.
	public int num_rows = 0;
	public int num_cols = 0;
	// Grid cell of the prefab. Row major.
	public Cell[] cells = null;
	// Parsed prefab json object.
	public JSONObject json_prefab;
	// Palette. Contains both the items defined in the prefab, and the items in the remote palette.
	public Palette palette;
	// Specify what to render and what to hide.
	public RenderFilter render_filter = new RenderFilter();
	// Show a cell grid.
	public boolean show_grid = false;

	public static class Label {
		public int x_px, y_px;
		public int col, row;
		public String value;
		public Color color;

		public Label(String value, int x_px, int y_px, int col, int row, Color color) {
			this.value = value;
			this.x_px = x_px;
			this.y_px = y_px;
			this.col = col;
			this.row = row;
			this.color = color;
		}
	}

	public static class Cell {
		// The tiles in the cells. From the character, items, monsters, etc.
		public ArrayList<Tiles.RenderTile> tiles = new ArrayList<>();
		// The character as defined in "row".
		public char character;
		// Is the cell character is unknown.
		public boolean unknow_cell_character = false;
	}

	public PrefabRendering(JSONObject json_prefab, Palette palette) {
		this.json_prefab = json_prefab;
		this.palette = palette;
		render_filter.setAllVisible();
	}

	// Get a cell index from its coordinates.
	public int cellIdx(int row, int col) {
		return col + row * num_cols;
	}

	// Recompute the rendering structure i.e. parse the json object.
	public void update() throws Exception {
		JSONObject json_object = (JSONObject) json_prefab.get("object");
		// Terrain.
		updateTerrain(json_object);
		// Items.
		updateItems(json_object);
	}

	public void updateTerrain(JSONObject json_object) throws Exception {
		JSONArray json_row = (JSONArray) json_object.get("rows");
		String fill_ter = (String) json_object.get("fill_ter");
		num_rows = json_row.size();
		num_cols = ((String) json_row.get(0)).length();
		cells = new Cell[num_rows * num_cols];
		// For all cell in the prefab.
		for (int row_idx = 0; row_idx < json_row.size(); row_idx++) {
			String row = (String) json_row.get(row_idx);
			if (row.length() != num_cols) {
				throw new Exception("The row " + (row_idx + 1)
						+ " does not have the same number of columns are the row 1.");
			}
			for (int col_idx = 0; col_idx < row.length(); col_idx++) {
				Cell cell = new Cell();
				cells[cellIdx(row_idx, col_idx)] = cell;
				cell.character = row.charAt(col_idx);
				if (fill_ter != null) {
					if (render_filter.isLayerVisible(eLayer.FILL_TER)) {
						Resources.tiles.appendTiles(cell.tiles, fill_ter, json_row, row_idx,
								col_idx, false, render_filter, palette);
					}
				}
				Palette.Cell palette_cell = palette.char_to_id.get(cell.character);
				if (palette_cell != null) {
					Resources.tiles.appendTiles(cell.tiles, palette_cell, json_row, row_idx,
							col_idx, render_filter, palette);
				} else if (cell.character != ' ' && cell.character != '.') {
					cell.unknow_cell_character = true;
				}
			}
		}
	}

	public void updateItems(JSONObject json_object) {
		JSONArray set = (JSONArray) json_object.get("set");
		if (set != null) {
			for (int item_idx = 0; item_idx < set.size(); item_idx++) {
				JSONObject item = (JSONObject) set.get(item_idx);
				renderItem(json_object, item);
			}
		}
		JSONArray place_items = (JSONArray) json_object.get("place_items");
		if (place_items != null) {
			for (int item_idx = 0; item_idx < place_items.size(); item_idx++) {
				JSONObject item = (JSONObject) place_items.get(item_idx);
				renderItem(json_object, item);
			}
		}
		JSONArray place_monsters = (JSONArray) json_object.get("place_monsters");
		if (place_monsters != null) {
			for (int item_idx = 0; item_idx < place_monsters.size(); item_idx++) {
				JSONObject item = (JSONObject) place_monsters.get(item_idx);
				renderItem(json_object, item);
			}
		}
	}

	// Generate a list of position from a line position json object.
	void genCandidateLinePositions(JSONObject item, ArrayList<Vector2i> candidates) {
		int x1 = (int) (long) item.get("x");
		int y1 = (int) (long) item.get("y");
		int x2 = (int) (long) item.get("x2");
		int y2 = (int) (long) item.get("y2");
		Geometry.callOnLine(new Vector2i(x1, y1), new Vector2i(x2, y2),
				new Geometry.PointCallback() {
					@Override
					public void iter(int x, int y) {
						candidates.add(new Vector2i(x, y));
					}
				});
	}

	// Generate a list of position from a generic position json object.
	void genCandidateRectPositions(JSONObject item, ArrayList<Vector2i> candidates) {
		int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		Object json_x = item.get("x");
		if (json_x instanceof JSONArray) {
			JSONArray xs = (JSONArray) json_x;
			if (xs.size() > 0) {
				x1 = (int) (long) xs.get(0);
			}
			if (xs.size() > 1) {
				x2 = (int) (long) xs.get(1);
			}
		} else if (json_x instanceof Long) {
			x1 = x2 = (int) (long) json_x;
		} else {
			Logger.fatal("Unsupported type:" + json_x);
		}
		Object json_y = item.get("y");
		if (json_y instanceof JSONArray) {
			JSONArray ys = (JSONArray) json_y;
			if (ys.size() > 0) {
				y1 = (int) (long) ys.get(0);
			}
			if (ys.size() > 1) {
				y2 = (int) (long) ys.get(1);
			}
		} else if (json_y instanceof Long) {
			y1 = y2 = (int) (long) json_y;
		} else {
			Logger.fatal("Unsupported type:" + json_y);
		}
		if (x1 > x2) {
			int swap = x1;
			x1 = x2;
			x2 = swap;
		}
		if (y1 > y2) {
			int swap = y1;
			y1 = y2;
			y2 = swap;
		}
		for (int y = y1; y <= y2; y++) {
			for (int x = x1; x <= x2; x++) {
				candidates.add(new Vector2i(x, y));
			}
		}
	}

	void renderItem(JSONObject json_object, JSONObject item) {
		// Get the candidate positions.
		ArrayList<Vector2i> candidates = new ArrayList<Vector2i>();
		String alt_id = null;
		String line = (String) item.get("line");
		if (line != null) {
			if (render_filter.isLayerVisible(eLayer.POINTS_AND_LINES)) {
				alt_id = line;
				genCandidateLinePositions(item, candidates);
			}
		} else {
			String point = (String) item.get("point");
			String json_item = (String) item.get("item");
			String monster = (String) item.get("monster");
			if (monster != null) {
				if (render_filter.isLayerVisible(eLayer.MONSTERS)) {
					alt_id = monster;
				}
			}
			if (point != null) {
				if (render_filter.isLayerVisible(eLayer.POINTS_AND_LINES)) {
					alt_id = point;
				}
			}
			if (json_item != null) {
				if (render_filter.isLayerVisible(eLayer.ITEMS)) {
					alt_id = json_item;
				}
			}
			if (monster != null || point != null || json_item != null) {
				genCandidateRectPositions(item, candidates);
			}
		}
		if (candidates.isEmpty())
			return;

		JSONArray json_row = (JSONArray) json_object.get("rows");
		String id = (String) item.get("id");
		if (id == null) {
			id = alt_id;
		}
		if (id == null) {
			return;
		}

		// TODO: Make the sampling more efficient.

		// If true, the positions are random and might differ from one instantiation to another.
		boolean candidate_are_random = false;

		// Sub-sampling of the candidate positions with "repeat".
		int num_to_keep = 0;
		Object repeat_object = item.get("repeat");
		if (repeat_object instanceof Long) {
			num_to_keep = (int) (long) repeat_object;
		} else if (repeat_object instanceof JSONArray) {
			JSONArray repeat = (JSONArray) repeat_object;
			int repeat_min = (int) (long) repeat.get(0);
			int repeat_max = (int) (long) repeat.get(1);
			if (repeat_min > repeat_max) {
				int swap = repeat_max;
				repeat_max = repeat_min;
				repeat_min = swap;
			}
			num_to_keep = Resources.random.nextInt(repeat_max - repeat_min + 1) + repeat_min;
		}
		if (num_to_keep < candidates.size()) {
			candidate_are_random = true;
			int num_to_remove = candidates.size() - num_to_keep;
			for (int remove_idx = 0; remove_idx < num_to_remove
					&& !candidates.isEmpty(); remove_idx++) {
				int remove_idx2 = Resources.random.nextInt(candidates.size());
				candidates.remove(remove_idx2);
			}
		}

		Long chance = (Long) item.get("chance");
		if (chance != null) {
			candidate_are_random = true;
		}
		for (Vector2i item_position : candidates) {
			renderItem(id, chance, item_position.x, item_position.y, json_row,
					candidate_are_random);
		}

	}

	private void renderItem(String id, Long chance, int x, int y, JSONArray json_row,
			boolean candidate_are_random) {
		if (chance != null) {
			// The rejection probability is "1-chance/100".
			if ((int) (long) chance < Resources.random.nextInt(100))
				return;
		}
		Cell cell = cells[cellIdx(y, x)];
		Resources.tiles.appendTiles(cell.tiles, id, json_row, x, y, candidate_are_random,
				render_filter, palette);
	}

	public void render(Graphics2D dst, int x, int y, int w, int h) {
		dst.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		if (Resources.tiles.iso) {
			renderIso(dst, x, y, w, h);
		} else {
			renderOrthogonal(dst, x, y, w, h);
		}
	}

	// Convert a cell position into a screen coordinate for the iso project.
	int isoCellToScreenX(int x, int y) {
		return x / 2 + y / 2 * 4 / 4;
	}

	int isoCellToScreenY(int x, int y, int cell_drawing_size_in_px) {
		int global_offset = num_rows * cell_drawing_size_in_px / 2;
		return y / 5 - x / 4 + global_offset;
	}

	// Render a iso tileset.
	// TODO: Make it work.
	public void renderIso(Graphics2D dst, int x, int y, int w, int h) {
		// The labels should be rendered at the end.
		ArrayList<Label> labels = new ArrayList<>();

		int cell_drawing_size_in_px = w / num_cols;
		for (int row = 0; row < num_rows; row++) {
			for (int col = num_cols - 1; col >= 0; col--) {
				Cell cell = cells[cellIdx(row, col)];
				int cell_x = col * cell_drawing_size_in_px + x;
				int cell_y = row * cell_drawing_size_in_px + y;
				for (RenderTile render_tile : cell.tiles) {
					Resources.tiles.render(dst, labels, render_tile,
							isoCellToScreenX(cell_x, cell_y),
							isoCellToScreenY(cell_x, cell_y, cell_drawing_size_in_px),
							cell_drawing_size_in_px * Resources.tiles.pixelscale / 2,
							cell_drawing_size_in_px * Resources.tiles.pixelscale / 2, col, row);
				}
				if (cell.unknow_cell_character) {
					// The cell symbol is unknown.
					labels.add(new Label(Character.toString(cell.character),
							isoCellToScreenX(cell_x + cell_drawing_size_in_px / 2 / 2, cell_y),
							isoCellToScreenY(cell_x, cell_y + cell_drawing_size_in_px / 2 / 2,
									cell_drawing_size_in_px),
							col, row, Color.RED));
				}
			}
		}

		// Background around labels.
		for (Label label : labels) {
			dst.setColor(label.color);
			int cell_x = label.col * cell_drawing_size_in_px + x;
			int cell_y = label.row * cell_drawing_size_in_px + y;
			dst.drawRect(isoCellToScreenX(cell_x, cell_y),
					isoCellToScreenY(cell_x, cell_y, cell_drawing_size_in_px),
					cell_drawing_size_in_px, cell_drawing_size_in_px);
		}

		// Labels.
		FontRenderContext font_render_context = new FontRenderContext(null, false, false);
		for (Label label : labels) {
			TextLayout text_layout = new TextLayout(label.value, dst.getFont(),
					font_render_context);
			AffineTransform transform = new AffineTransform();
			transform.translate(isoCellToScreenX(label.x_px, label.y_px),
					isoCellToScreenY(label.x_px, label.y_px, cell_drawing_size_in_px));
			Shape outline = text_layout.getOutline(transform);
			BasicStroke wideStroke = new BasicStroke(2);
			dst.setColor(Color.BLACK);
			dst.setStroke(wideStroke);
			dst.draw(outline);
			dst.setColor(label.color);
			dst.drawString(label.value, isoCellToScreenX(label.x_px, label.y_px),
					isoCellToScreenY(label.x_px, label.y_px, cell_drawing_size_in_px));
		}
	}

	// Render the prefab with an orthogonal(i.e. classical) tileset.
	public void renderOrthogonal(Graphics2D dst, int x, int y, int w, int h) {
		// The labels should be rendered at the end.
		ArrayList<Label> labels = new ArrayList<>();

		int cell_drawing_size_in_px = w / num_cols;
		for (int row = 0; row < num_rows; row++) {
			for (int col = 0; col < num_cols; col++) {
				Cell cell = cells[cellIdx(row, col)];
				int cell_x = col * cell_drawing_size_in_px + x;
				int cell_y = row * cell_drawing_size_in_px + y;
				for (RenderTile render_tile : cell.tiles) {
					Resources.tiles.render(dst, labels, render_tile, cell_x, cell_y,
							cell_drawing_size_in_px * Resources.tiles.pixelscale,
							cell_drawing_size_in_px * Resources.tiles.pixelscale, col, row);
				}
				if (cell.unknow_cell_character) {
					// The cell symbol is unknown.
					labels.add(new Label(Character.toString(cell.character),
							cell_x + cell_drawing_size_in_px / 2,
							cell_y + cell_drawing_size_in_px / 2, col, row, Color.RED));
				}
			}
		}

		if (show_grid) {
			renderGridOrtho(dst, x, y, w, h);
		}

		// Background around labels.
		for (Label label : labels) {
			dst.setColor(label.color);
			int cell_x = label.col * cell_drawing_size_in_px + x;
			int cell_y = label.row * cell_drawing_size_in_px + y;
			dst.drawRect(cell_x, cell_y, cell_drawing_size_in_px, cell_drawing_size_in_px);
		}

		// Labels.
		FontRenderContext font_render_context = new FontRenderContext(null, false, false);
		for (Label label : labels) {
			TextLayout text_layout = new TextLayout(label.value, dst.getFont(),
					font_render_context);
			AffineTransform transform = new AffineTransform();
			transform.translate(label.x_px, label.y_px);
			Shape outline = text_layout.getOutline(transform);
			BasicStroke wideStroke = new BasicStroke(2);
			dst.setColor(Color.BLACK);
			dst.setStroke(wideStroke);
			dst.draw(outline);
			dst.setColor(label.color);
			dst.drawString(label.value, label.x_px, label.y_px);
		}
	}

	// Render a grid.
	private void renderGridOrtho(Graphics2D dst, int x, int y, int w, int h) {
		int cell_drawing_size_in_px = w / num_cols;

		Stroke save = dst.getStroke();
		Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
				new float[] { 2, 2 }, 0);
		dst.setStroke(dashed);

		dst.setColor(new Color(1.0f, 1.0f, 1.0f, 0.3f));
		for (int col = 0; col < num_cols; col++) {
			int x_px = col * cell_drawing_size_in_px + x;
			dst.drawLine(x_px, y, x_px, y + h);
		}
		for (int row = 0; row < num_rows; row++) {
			int y_px = row * cell_drawing_size_in_px + y;
			dst.drawLine(x, y_px, x + w, y_px);
		}
		dst.setColor(Color.WHITE);
		dst.setStroke(save);
	}

	// Create a text description of the cell. Used in the info box.
	public void appendCellDescription(int x, int y, ArrayList<String> description) {
		Cell cell = cells[cellIdx(y, x)];
		description.add(String.format("Coordinates: %d,%d", x, y));
		String label = String.format("Symbol: '%c'", cell.character);
		if (cell.unknow_cell_character) {
			// The cell symbol is unknown.
			label += " [unknown symbol]";
		}
		description.add(label);
		boolean candidate_are_random = false;
		for (RenderTile tile : cell.tiles) {
			if (tile.candidate_are_random) {
				candidate_are_random = true;
				break;
			}
		}
		if (candidate_are_random) {
			description.add("Contains random content");
		}
		for (RenderTile tile : cell.tiles) {
			description.add(tile.symbol);
		}
	}

	// Switch on/off the display of the grid.
	public void setShowGrid(boolean show) {
		show_grid = show;
	}

	// Get the cell data from its coordinates.
	public Cell getCell(Vector2i cell_coordinate) {
		return cells[cellIdx(cell_coordinate.y, cell_coordinate.x)];
	}

}
