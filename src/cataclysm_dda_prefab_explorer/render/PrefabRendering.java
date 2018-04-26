
package cataclysm_dda_prefab_explorer.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import cataclysm_dda_prefab_explorer.resources_loader.Palette;
import cataclysm_dda_prefab_explorer.resources_loader.Resources;
import cataclysm_dda_prefab_explorer.resources_loader.Tiles;
import cataclysm_dda_prefab_explorer.resources_loader.Tiles.RenderTile;
import cataclysm_dda_prefab_explorer.utils.Geometry;
import cataclysm_dda_prefab_explorer.utils.Logger;
import cataclysm_dda_prefab_explorer.utils.Vector2i;

// The project of a prefab object into something ready to be rendered.
public class PrefabRendering {

	static class Cell {
		public ArrayList<Tiles.RenderTile> tiles = new ArrayList<Tiles.RenderTile>();
		public char character;
		// Is the cell character is unknown.
		public boolean unknow_cell_character = false;
	};

	public int num_rows = 0;
	public int num_cols = 0;
	public Cell[] cells = null;
	public JSONObject json_prefab;
	public Palette palette;

	public PrefabRendering(JSONObject json_prefab, Palette palette) {
		this.json_prefab = json_prefab;
		this.palette = palette;
	}

	public int cellIdx(int row, int col) {
		return col + row * num_cols;
	}

	public void update() {
		JSONObject json_object = (JSONObject) json_prefab.get("object");
		// Terrain.
		updateTerrain(json_object);
		// Items.
		updateItems(json_object);
	}

	public void updateTerrain(JSONObject json_object) {
		JSONArray json_row = (JSONArray) json_object.get("rows");
		String fill_ter = (String) json_object.get("fill_ter");
		num_rows = json_row.size();
		num_cols = ((String) json_row.get(0)).length();
		cells = new Cell[num_rows * num_cols];
		// For all cell in the prefab.
		for (int row_idx = 0; row_idx < json_row.size(); row_idx++) {
			String row = (String) json_row.get(row_idx);
			for (int col_idx = 0; col_idx < row.length(); col_idx++) {
				Cell cell = new Cell();
				cells[cellIdx(row_idx, col_idx)] = cell;
				cell.character = row.charAt(col_idx);
				if (fill_ter != null) {
					Resources.tiles.appendTiles(cell.tiles, fill_ter, json_row, row_idx, col_idx);
				}
				Palette.Cell palette_cell = palette.char_to_id.get(cell.character);
				if (palette_cell != null) {
					Resources.tiles.appendTiles(cell.tiles, palette_cell, json_row, row_idx,
							col_idx);
				} else {
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
		// Candidate positions.
		ArrayList<Vector2i> candidates = new ArrayList<Vector2i>();
		String alt_id = null;
		String line = (String) item.get("line");
		if (line != null) {
			alt_id = line;
			genCandidateLinePositions(item, candidates);
		} else {
			String point = (String) item.get("point");
			String json_item = (String) item.get("item");
			String monster = (String) item.get("monster");
			if (monster != null) {
				alt_id = monster;
			}
			if (point != null) {
				alt_id = point;
			}
			if (json_item != null) {
				alt_id = json_item;
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

		// TODO: Make the sampling more efficient.

		// Random sampling.
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
			int num_to_remove = candidates.size() - num_to_keep;
			for (int remove_idx = 0; remove_idx < num_to_remove
					&& !candidates.isEmpty(); remove_idx++) {
				int remove_idx2 = Resources.random.nextInt(candidates.size());
				candidates.remove(remove_idx2);
			}
		}

		Long chance = (Long) item.get("chance");
		for (Vector2i item_position : candidates) {
			renderItem(id, chance, item_position.x, item_position.y, json_row);
		}

	}

	private void renderItem(String id, Long chance, int x, int y, JSONArray json_row) {
		if (chance != null) {
			if ((int) (long) chance < Resources.random.nextInt(100))
				return;
		}
		Cell cell = cells[cellIdx(y, x)];
		Resources.tiles.appendTiles(cell.tiles, id, json_row, x, y);
	}

	public void render(Graphics2D dst, int x, int y, int w, int h) {
		int cell_drawing_size_in_px = w / num_cols;
		for (int row = 0; row < num_rows; row++) {
			for (int col = 0; col < num_cols; col++) {
				Cell cell = cells[cellIdx(row, col)];
				int cell_x = col * cell_drawing_size_in_px + x;
				int cell_y = row * cell_drawing_size_in_px + y;
				for (RenderTile render_tile : cell.tiles) {
					Resources.tiles.render(dst, render_tile, cell_x, cell_y,
							cell_drawing_size_in_px, cell_drawing_size_in_px);
				}
				if (cell.unknow_cell_character) {
					// The cell symbol is unknown.
					dst.setColor(new Color(255, 0, 0));
					dst.drawString(Character.toString(cell.character),
							cell_x + cell_drawing_size_in_px / 2,
							cell_y + cell_drawing_size_in_px / 2);
				}
			}
		}
	}

	public void appendCellDescription(int x, int y, ArrayList<String> description) {
		Cell cell = cells[cellIdx(y, x)];
		description.add(String.format("Coordinates: %d,%d", x, y));
		if (cell.unknow_cell_character) {
			// The cell symbol is unknown.
			description.add(String.format("Symbol: '%c' [Unknown]", cell.character));
		} else {
			description.add(String.format("Symbol: '%c'", cell.character));
		}
		for (RenderTile tile : cell.tiles) {
			description.add(tile.symbol);
		}
	}

}
