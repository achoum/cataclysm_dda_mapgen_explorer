
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
import java.util.HashSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mapgen_explorer.render.shape.AbstractShape;
import mapgen_explorer.render.shape.Line;
import mapgen_explorer.render.shape.Rect;
import mapgen_explorer.resources_loader.Palette;
import mapgen_explorer.resources_loader.Palette.Item;
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
	// Shapes.
	public ArrayList<AbstractShape> shapes = new ArrayList<>();
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
		public eLayer layer;

		public Label(String value, int x_px, int y_px, int col, int row, eLayer layer) {
			this.value = value;
			this.x_px = x_px;
			this.y_px = y_px;
			this.col = col;
			this.row = row;
			this.layer = layer;
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
	public int cellIdx(int row, int col) throws Exception {
		if (row < 0 || row >= num_rows || col < 0 || col >= num_cols) {
			throw new Exception("Out of bound cell index " + row + ";" + col);
		}
		return col + row * num_cols;
	}

	// Recompute the rendering structure i.e. parse the json object.
	public void update() throws Exception {
		shapes.clear();
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

				if (cell.character == '#') {
					int a = 50;
				}

				ArrayList<Item> palette_item_set = palette.char_to_id.get(cell.character);
				if (palette_item_set == null) {
					Resources.tiles.appendTilesFromSymbol(cell.tiles, fill_ter, json_row, row_idx,
							col_idx, false, palette, RenderFilter.eLayer.TERRAIN);
					if (cell.character != ' ' && cell.character != '.') {
						Logger.consoleWarnings(
								"Unknown symbol \"" + Character.toString(cell.character) + "\"");
						Resources.tiles.appendTilesFromSymbol(cell.tiles,
								Character.toString(cell.character), json_row, row_idx, col_idx,
								false, palette, RenderFilter.eLayer.TERRAIN);
					}
				} else {
					boolean has_terrain_palette_item = false;
					for (Item palette_item : palette_item_set) {
						if (palette_item.layer == eLayer.TERRAIN) {
							has_terrain_palette_item = true;
							break;
						}
					}
					if (fill_ter != null && !has_terrain_palette_item) {
						Resources.tiles.appendTilesFromSymbol(cell.tiles, fill_ter, json_row,
								row_idx, col_idx, false, palette, RenderFilter.eLayer.TERRAIN);
					}

					boolean terrain_filled = false;

					for (Item palette_item : palette_item_set) {
						if (palette_item != null) {

							if (palette_item.layer == eLayer.TERRAIN && terrain_filled) {
								continue;
							}

							boolean success = Resources.tiles.appendTilesFromPaletteItem(cell.tiles,
									palette_item, json_row, row_idx, col_idx, palette,
									palette_item.layer);

							if (palette_item.layer == eLayer.TERRAIN) {
								terrain_filled = true;
							}

							if (!success && palette_item.layer == eLayer.TERRAIN) {
								Resources.tiles.appendTilesFromSymbol(cell.tiles, fill_ter,
										json_row, row_idx, col_idx, false, palette,
										RenderFilter.eLayer.TERRAIN);
							}

						} else if (cell.character != ' ' && cell.character != '.') {
							// Unknown symbol.
							cell.unknow_cell_character = true;
						}
					}

					if (cell.tiles.isEmpty()) {
						Resources.tiles.appendTilesFromSymbol(cell.tiles, fill_ter, json_row,
								row_idx, col_idx, false, palette, RenderFilter.eLayer.TERRAIN);
					}

				}
			}
		}
	}

	public void updateItems(JSONObject json_object) throws Exception {

		updatItems(json_object, "set", RenderFilter.eLayer.TERRAIN, null);
		updatItems(json_object, "add", RenderFilter.eLayer.ITEMS, null);
		updatItems(json_object, "place_terrain", RenderFilter.eLayer.TERRAIN, null);
		updatItems(json_object, "place_items", RenderFilter.eLayer.ITEMS, null);
		updatItems(json_object, "place_loot", RenderFilter.eLayer.LOOT, null);
		updatItems(json_object, "place_monsters", RenderFilter.eLayer.MONSTER, null);
		updatItems(json_object, "place_monster", RenderFilter.eLayer.MONSTER, null);
		updatItems(json_object, "place_npcs", RenderFilter.eLayer.NPC, null);
		updatItems(json_object, "place_npc", RenderFilter.eLayer.NPC, null);
		updatItems(json_object, "place_fields", RenderFilter.eLayer.FIELD, null);
		updatItems(json_object, "place_rubble", RenderFilter.eLayer.TERRAIN, "rubble");
		updatItems(json_object, "place_vehicles", RenderFilter.eLayer.VEHICULE, null);
		updatItems(json_object, "place_vendingmachines", RenderFilter.eLayer.VENDING_MACHINE,
				"vendingmachines");
		updatItems(json_object, "place_signs", RenderFilter.eLayer.SIGN, "sign");
		updatItems(json_object, "place_liquids", RenderFilter.eLayer.LIQUID, null);
		updatItems(json_object, "place_toilets", RenderFilter.eLayer.TOILETS, "toilet");
		updatItems(json_object, "place_gaspumps", RenderFilter.eLayer.GASPUMP, "gaspump");
		updatItems(json_object, "place_furniture", RenderFilter.eLayer.FURNITURE, null);
		updatItems(json_object, "place_traps", RenderFilter.eLayer.TRAP, null);

		HashSet<String> known_placements = new HashSet<>();
		known_placements.add("set");
		known_placements.add("add");
		known_placements.add("place_terrain");
		known_placements.add("place_items");
		known_placements.add("place_loot");
		known_placements.add("place_monsters");
		known_placements.add("place_monster");
		known_placements.add("place_npcs");
		known_placements.add("place_npc");
		known_placements.add("place_fields");
		known_placements.add("place_rubble");
		known_placements.add("place_vehicles");
		known_placements.add("place_vendingmachines");
		known_placements.add("place_signs");
		known_placements.add("place_liquids");
		known_placements.add("place_toilets");
		known_placements.add("place_gaspumps");
		known_placements.add("place_furniture");
		known_placements.add("place_traps");

		for (Object entry : json_object.keySet()) {
			String str_entry = (String) entry;
			if (str_entry.startsWith("place_") && !known_placements.contains(str_entry)) {
				Logger.consoleWarnings(
						"Unknown placement type: " + str_entry + " : " + json_object.get(entry));
				updatItems(json_object, str_entry, RenderFilter.eLayer.ITEMS, str_entry + " [U]");
			}
		}

	}

	void updatItems(JSONObject json_object, String place_key, RenderFilter.eLayer layer,
			String last_resort_name) throws Exception {
		JSONArray places = (JSONArray) json_object.get(place_key);
		if (places != null) {
			for (int item_idx = 0; item_idx < places.size(); item_idx++) {
				JSONObject item = (JSONObject) places.get(item_idx);
				renderItem(json_object, item, layer, last_resort_name);
			}
		}
	}

	// Generate a list of position from a line position json object.
	Line genCandidateLinePositions(JSONObject item, ArrayList<Vector2i> candidates) {
		Line line = new Line();
		line.x1 = (int) (long) item.get("x");
		line.y1 = (int) (long) item.get("y");
		line.x2 = (int) (long) item.get("x2");
		line.y2 = (int) (long) item.get("y2");
		Geometry.callOnLine(new Vector2i(line.x1, line.y1), new Vector2i(line.x2, line.y2),
				new Geometry.PointCallback() {
					@Override
					public void iter(int x, int y) {
						candidates.add(new Vector2i(x, y));
					}
				});
		return line;
	}

	// Generate a list of position from a generic position json object.
	Rect genCandidateRectPositions(JSONObject item, ArrayList<Vector2i> candidates)
			throws Exception {
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
			throw new Exception("Unsupported type:" + json_x);
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
			throw new Exception("Unsupported type:" + json_y);
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
		Rect rect = new Rect();
		rect.x1 = x1;
		rect.x2 = x2;
		rect.y1 = y1;
		rect.y2 = y2;
		return rect;
	}

	void renderItem(JSONObject json_object, JSONObject item, RenderFilter.eLayer layer,
			String last_resort_name) throws Exception {

		// Get the candidate positions.
		ArrayList<Vector2i> candidates = new ArrayList<Vector2i>();
		String final_id = null;

		AbstractShape shape = null;

		if (item.containsKey("line")) {
			String line = (String) item.get("line");
			final_id = line;
			shape = genCandidateLinePositions(item, candidates);
		} else if (item.containsKey("point")) {
			String point = (String) item.get("point");
			final_id = point;
			shape = genCandidateRectPositions(item, candidates);
		} else if (item.containsKey("square")) {
			String square = (String) item.get("square");
			final_id = square;
			shape = genCandidateRectPositions(item, candidates);
		} else {
			final_id = (String) item.get(layer.key_id);
			shape = genCandidateRectPositions(item, candidates);
		}

		if (item.containsKey("id")) {
			final_id = (String) item.get("id");
		}

		if (final_id == null) {
			for (String new_try : new String[] { "group" }) {
				String candidate_id = (String) item.get(new_try);
				if (candidate_id != null) {
					final_id = candidate_id;
					break;
				}
			}
		}

		if (final_id == null) {
			if (last_resort_name != null) {
				final_id = last_resort_name;
			} else {
				Logger.consoleWarnings("Cannot find the key for : " + item);
				final_id = "unknown";
			}
		}

		shape.label = final_id;
		shape.layer = layer;

		JSONArray json_row = (JSONArray) json_object.get("rows");

		// TODO: Make the sampling more efficient.

		// If true, the positions are random and might differ from one instantiation to another.
		boolean candidate_are_random = false;

		// Sub-sampling of the candidate positions with "repeat".
		int num_to_keep = -1;
		Object repeat_object = item.get("repeat");
		if (repeat_object instanceof Long) {
			num_to_keep = (int) (long) repeat_object;
			shape.label += " [repeat: " + num_to_keep + "]";
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
			shape.label += " [repeat: " + repeat_min + ";" + repeat_max + "]";
		}

		if (final_id.equals("GROUP_ZOMBIE") && num_to_keep == -1) {
			num_to_keep = 1;
		}

		if (num_to_keep != -1 && num_to_keep < candidates.size()) {
			candidate_are_random = true;
			int num_to_remove = candidates.size() - num_to_keep;
			for (int remove_idx = 0; remove_idx < num_to_remove
					&& !candidates.isEmpty(); remove_idx++) {
				int remove_idx2 = Resources.random.nextInt(candidates.size());
				candidates.remove(remove_idx2);
			}

		}

		int chance = Palette.extractChance(item, shape);

		if (layer == eLayer.MONSTER) {
			// In the game, this seems to be impacted by the distance to the closest city.
			chance = (chance + 1) * 10;
		}

		shapes.add(shape);
		for (Vector2i item_position : candidates) {
			renderItem(final_id, chance, item_position.x, item_position.y, json_row,
					candidate_are_random, layer);
		}

	}

	private void renderItem(String id, int chance, int x, int y, JSONArray json_row,
			boolean candidate_are_random, RenderFilter.eLayer layer) throws Exception {
		if (chance < Resources.random.nextInt(100))
			return;
		int cell_idx = cellIdx(y, x);
		Cell cell = cells[cell_idx];
		Resources.tiles.appendTilesFromSymbol(cell.tiles, id, json_row, x, y, candidate_are_random,
				palette, layer);
	}

	public void render(Graphics2D dst, int x, int y, int w, int h) throws Exception {
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
	public void renderIso(Graphics2D dst, int x, int y, int w, int h) throws Exception {
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
							col, row, eLayer.UNKNOWN));
				}
			}
		}

		// Background around labels.
		for (Label label : labels) {
			dst.setColor(label.layer.color);
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
			dst.setColor(label.layer.color);
			dst.drawString(label.value, isoCellToScreenX(label.x_px, label.y_px),
					isoCellToScreenY(label.x_px, label.y_px, cell_drawing_size_in_px));
		}
	}

	// Render the prefab with an orthogonal(i.e. classical) tileset.
	public void renderOrthogonal(Graphics2D dst, int x, int y, int w, int h) throws Exception {
		// The labels should be rendered at the end.
		ArrayList<Label> labels = new ArrayList<>();

		int cell_drawing_size_in_px = w / num_cols;
		for (int row = 0; row < num_rows; row++) {
			for (int col = 0; col < num_cols; col++) {
				Cell cell = cells[cellIdx(row, col)];
				int cell_x = col * cell_drawing_size_in_px + x;
				int cell_y = row * cell_drawing_size_in_px + y;
				if (Resources.tiles.force_character) {
					dst.setColor(new Color(200, 200, 200));
					dst.drawString(Character.toString(cell.character),
							cell_x + cell_drawing_size_in_px / 2
									- dst.getFontMetrics().charWidth(cell.character) / 2,
							cell_y - cell_drawing_size_in_px / 2 + 3);
				} else {

					for (RenderTile render_tile : cell.tiles) {
						if (!render_filter.isLayerVisible(render_tile.layer)) {
							continue;
						}
						Resources.tiles.render(dst, labels, render_tile, cell_x, cell_y,
								cell_drawing_size_in_px * Resources.tiles.pixelscale,
								cell_drawing_size_in_px * Resources.tiles.pixelscale, col, row);
					}
					if (cell.unknow_cell_character) {
						if (!render_filter.isLayerVisible(RenderFilter.eLayer.UNKNOWN)) {
							continue;
						}
						// The cell symbol is unknown.
						labels.add(new Label(Character.toString(cell.character),
								cell_x + cell_drawing_size_in_px / 2,
								cell_y + cell_drawing_size_in_px / 2, col, row, eLayer.UNKNOWN));
					}
				}
			}
		}

		if (show_grid || Resources.tiles.force_character) {
			renderGridOrtho(dst, x, y, w, h);
		}

		// Background around labels.
		for (Label label : labels) {
			dst.setColor(label.layer.color);
			int cell_x = label.col * cell_drawing_size_in_px + x;
			int cell_y = label.row * cell_drawing_size_in_px + y;
			dst.drawRect(cell_x, cell_y, cell_drawing_size_in_px, cell_drawing_size_in_px);
		}

		FontRenderContext font_render_context = new FontRenderContext(null, false, false);

		// Shapes
		Stroke save = dst.getStroke();
		Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
				new float[] { 2, 2 }, 0);

		for (AbstractShape shape : shapes) {

			if (!render_filter.isLayerVisible(shape.layer)) {
				continue;
			}

			// Get the label position.
			int label_x = 0, label_y = 0;
			if (shape instanceof Line) {
				Line line = (Line) shape;
				label_x = line.x1 * cell_drawing_size_in_px + cell_drawing_size_in_px / 2;
				label_y = line.y1 * cell_drawing_size_in_px + cell_drawing_size_in_px / 2;
			} else if (shape instanceof Rect) {
				Rect rect = (Rect) shape;
				label_x = rect.x1 * cell_drawing_size_in_px;
				label_y = rect.y1 * cell_drawing_size_in_px;
			}
			label_y += 13;
			label_x += 3;
			// Label background.
			TextLayout text_layout = new TextLayout(shape.toString(), dst.getFont(),
					font_render_context);
			AffineTransform transform = new AffineTransform();
			transform.translate(label_x, label_y);
			Shape outline = text_layout.getOutline(transform);
			BasicStroke wideStroke = new BasicStroke(2);
			dst.setColor(Color.BLACK);
			dst.setStroke(wideStroke);
			dst.draw(outline);
			// Shape.
			dst.setStroke(dashed);
			dst.setColor(shape.layer.color);
			if (shape instanceof Line) {
				Line line = (Line) shape;
				dst.drawLine(line.x1 * cell_drawing_size_in_px + cell_drawing_size_in_px / 2,
						line.y1 * cell_drawing_size_in_px + cell_drawing_size_in_px / 2,
						line.x2 * cell_drawing_size_in_px + cell_drawing_size_in_px / 2,
						line.y2 * cell_drawing_size_in_px + cell_drawing_size_in_px / 2);
			} else if (shape instanceof Rect) {
				Rect rect = (Rect) shape;
				dst.drawRect(rect.x1 * cell_drawing_size_in_px, rect.y1 * cell_drawing_size_in_px,
						(rect.x2 - rect.x1 + 1) * cell_drawing_size_in_px,
						(rect.y2 - rect.y1 + 1) * cell_drawing_size_in_px);
			}
			dst.setStroke(save);
			// Label
			dst.setColor(shape.layer.color);
			dst.drawString(shape.toString(), label_x, label_y);
		}

		// Labels.
		for (Label label : labels) {
			if (!render_filter.isLayerVisible(label.layer)) {
				continue;
			}

			TextLayout text_layout = new TextLayout(label.value, dst.getFont(),
					font_render_context);
			AffineTransform transform = new AffineTransform();
			transform.translate(label.x_px, label.y_px);
			Shape outline = text_layout.getOutline(transform);
			BasicStroke wideStroke = new BasicStroke(2);
			dst.setColor(Color.BLACK);
			dst.setStroke(wideStroke);
			dst.draw(outline);
			dst.setColor(label.layer.color);
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
	public void appendCellDescription(int x, int y, ArrayList<String> description)
			throws Exception {
		Cell cell = cells[cellIdx(y, x)];
		// Coordinates.
		description.add(String.format("x:%d y:%d", x, y));
		// Symbol.
		String label = String.format("Symbol: '%c'", cell.character);
		if (cell.unknow_cell_character) {
			// The cell symbol is unknown.
			label += " [unknown symbol]";
		}
		description.add(label);
		// Check for random tiles.
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

		// Tiles.
		if (!cell.tiles.isEmpty()) {
			description.add("");
			description.add("Tile");
			for (RenderTile tile : cell.tiles) {
				description.add(tile.toString());
			}
		}

		// Shapes.
		boolean has_shape = false;
		for (AbstractShape shape : shapes) {
			if (shape.contains(x, y)) {
				has_shape = true;
				break;
			}
		}
		if (has_shape) {
			description.add("");
			description.add("Place");
			for (AbstractShape shape : shapes) {
				if (!shape.contains(x, y)) {
					continue;
				}
				description.add(shape.label + " [layer:" + shape.layer.label + "]");
			}
		}

	}

	// Switch on/off the display of the grid.
	public void setShowGrid(boolean show) {
		show_grid = show;
	}

	// Get the cell data from its coordinates.
	public Cell getCell(Vector2i cell_coordinate) throws Exception {
		return cells[cellIdx(cell_coordinate.y, cell_coordinate.x)];
	}

}
