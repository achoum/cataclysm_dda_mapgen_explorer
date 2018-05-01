
package mapgen_explorer.resources_loader;

import mapgen_explorer.render.PrefabRendering;
import mapgen_explorer.resources_loader.Palette.Item;
import mapgen_explorer.utils.FileUtils;
import mapgen_explorer.utils.RenderFilter;
import mapgen_explorer.utils.RenderFilter.eLayer;
import mapgen_explorer.utils.Vector2i;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

// Mapping from a display symbol (e.g. "t_grass") to the sprite. Contains various functions to help the rendering.
public class Tiles {

	HashMap<String, Tile> tiles = new HashMap<>();
	public boolean iso = false;
	public int pixelscale = 1;
	public boolean force_character = false;
	public int base_size_x = 1;
	public int base_size_y = 1;

	public static class TileVariant {
		public TileVariant(int sprite, int tile_offset) {
			this.sprite = sprite - tile_offset;
		}

		public int sprite;
	}

	public static class RotationAndTileOffset {
		// Expressed in multiples of 90 degree.
		int rotation = 0;
		String alternative_key;

		public RotationAndTileOffset(int rotation, String alternative_key) {
			this.rotation = rotation;
			this.alternative_key = alternative_key;
		}
	}

	public static RotationAndTileOffset[] neighbor_map_to_rotation_rotate;
	public static RotationAndTileOffset[] neighbor_map_to_rotation_non_rotate;

	public static class Tile {
		public BufferedImage image;
		public ArrayList<TileVariant> foreground = new ArrayList<>();
		public ArrayList<TileVariant> background = new ArrayList<>();
		public HashMap<String, Tile> additional_tiles = new HashMap<>();
		public int tile_size_x_px, tile_size_y_px;
		public int offset_x_px, offset_y_px;
		public int num_tile_x, num_tile_y;
		public boolean rotates;
		public boolean multitile;
	}

	public static class RenderTile {
		public int rotation = 0;
		public int sprite = -1;
		public String symbol;
		public Tile tile;
		public Color label_color = Color.GREEN;
		public boolean candidate_are_random = false;
		public eLayer layer;

		private void RenderTile() {
		}

		@Override
		public String toString() {
			return symbol + " [" + layer.label + "]";
		}
	}

	String findBaseTileImageFilename(String tile_directory) throws Exception {
		File[] candidates = (new File(tile_directory)).listFiles();
		for (File candidate : candidates) {
			if (FileUtils.getExtension(candidate.getName()).equals("png")) {
				return candidate.getName();
			}
		}
		throw new Exception("No base tile image in " + tile_directory);
	}

	public static ArrayList<String> listTilesets(String main_directory) {
		ArrayList<String> tilesets = new ArrayList<>();
		String tile_directory = Paths.get(main_directory, "gfx").toString();
		for (File tile_name : (new File(tile_directory).listFiles())) {
			if (!tile_name.isDirectory())
				continue;
			tilesets.add(tile_name.getName());
		}
		return tilesets;
	}

	public void load(String main_directory, String tileset) throws Exception {
		if (tileset.equals("ASCII")) {
			Resources.tiles.force_character = true;
			return;
		}
		force_character = false;
		tiles.clear();
		String tile_directory = Paths.get(main_directory, "gfx", tileset).toString();
		String tile_config_path = Paths.get(tile_directory, "tile_config.json").toString();
		JSONParser parser = new JSONParser();
		FileReader reader = new FileReader(tile_config_path);
		JSONObject json_tile = (JSONObject) parser.parse(reader);
		String base_tile_filename = findBaseTileImageFilename(tile_directory);
		load(tile_directory, base_tile_filename, json_tile);
		reader.close();
	}

	void load(String tile_directory, String base_tile_filename, JSONObject json_tile)
			throws Exception {
		System.out.println("Loading tileset in " + tile_directory);
		JSONObject tile_info = (JSONObject) ((JSONArray) json_tile.get("tile_info")).get(0);
		// Default tile size i.e. unless re-specified with "tiles-new"
		base_size_x = (int) (long) tile_info.get("width");
		base_size_y = (int) (long) tile_info.get("height");
		pixelscale = (int) (long) tile_info.getOrDefault("pixelscale", 1L);
		iso = (Boolean) tile_info.getOrDefault("iso", Boolean.FALSE);
		if (iso) {
			pixelscale = 2;
		}

		JSONArray json_tiles = (JSONArray) json_tile.get("tiles");
		if (json_tiles != null) {
			loadTiles(json_tiles, Paths.get(tile_directory, base_tile_filename).toString(),
					base_size_x, base_size_y, 0, 0, 0);
		}
		JSONArray json_new_tiles = (JSONArray) json_tile.get("tiles-new");
		if (json_new_tiles != null) {
			int tile_idx_offset = 0;
			for (int new_tile_idx = 0; new_tile_idx < json_new_tiles.size(); new_tile_idx++) {
				JSONObject new_tile = (JSONObject) (json_new_tiles.get(new_tile_idx));
				JSONArray sub_tiles = (JSONArray) new_tile.get("tiles");
				int size_x = (int) (long) new_tile.getOrDefault("sprite_width", (long) base_size_x);
				int size_y = (int) (long) new_tile.getOrDefault("sprite_height",
						(long) base_size_y);
				int offset_x = (int) (long) new_tile.getOrDefault("sprite_offset_x", (long) 0);
				int offset_y = (int) (long) new_tile.getOrDefault("sprite_offset_y", (long) 0);
				tile_idx_offset = loadTiles(sub_tiles,
						Paths.get(tile_directory, (String) new_tile.get("file")).toString(), size_x,
						size_y, offset_x, offset_y, tile_idx_offset);
			}
		}
	}

	int loadTiles(JSONArray json_tiles, String file_image_path, int tile_size_x, int tile_size_y,
			int offset_x, int offset_y, int tile_idx_offset) throws Exception {
		FileInputStream image_reader = new FileInputStream(new File(file_image_path));
		BufferedImage image = ImageIO.read(new BufferedInputStream(image_reader));
		return loadTiles(json_tiles, image, tile_size_x, tile_size_y, offset_x, offset_y, tiles,
				tile_idx_offset);
	}

	int loadTiles(JSONArray json_tiles, BufferedImage image, int tile_size_x, int tile_size_y,
			int offset_x, int offset_y, HashMap<String, Tile> dst_tiles, int tile_idx_offset)
			throws Exception {
		int num_split_columns = image.getWidth() / tile_size_x;
		int num_split_rows = image.getHeight() / tile_size_y;
		for (int tile_idx = 0; tile_idx < json_tiles.size(); tile_idx++) {
			JSONObject json_tile = (JSONObject) json_tiles.get(tile_idx);
			Object tile_id = json_tile.get("id");
			if (tile_id == null) {
				System.out.println("Missing tile id. Skipping the tile.");
				continue;
			}
			Tile tile = new Tile();
			tile.image = image;
			Object json_foreground = json_tile.get("fg");
			if (json_foreground != null) {
				loadTile(json_foreground, num_split_columns, tile_size_x, tile_size_y,
						tile.foreground, tile_idx_offset);
			}
			Object json_background = json_tile.get("bg");
			if (json_background != null) {
				loadTile(json_background, num_split_columns, tile_size_x, tile_size_y,
						tile.background, tile_idx_offset);
			}

			tile.rotates = (Boolean) json_tile.getOrDefault("rotates", Boolean.FALSE);
			tile.multitile = (Boolean) json_tile.getOrDefault("multitile", Boolean.FALSE);

			JSONArray json_additional_tiles = (JSONArray) json_tile.get("additional_tiles");
			if (json_additional_tiles != null) {
				loadTiles(json_additional_tiles, image, tile_size_x, tile_size_y, offset_x,
						offset_y, tile.additional_tiles, tile_idx_offset);
			}

			tile.tile_size_x_px = tile_size_x;
			tile.tile_size_y_px = tile_size_y;
			tile.offset_x_px = offset_x;
			tile.offset_y_px = offset_y;
			tile.num_tile_x = image.getWidth() / tile_size_x;
			tile.num_tile_y = image.getHeight() / tile_size_y;
			registerTile(tile_id, tile, dst_tiles);
		}
		return tile_idx_offset + num_split_columns * num_split_rows;
	}

	private void loadTile(Object json_tile_object, int num_split_columns, int tile_size_x,
			int tile_size_y, ArrayList<TileVariant> variants, int tile_idx_offset)
			throws Exception {
		if (json_tile_object instanceof Long) {
			variants.add(new TileVariant((int) (long) json_tile_object, tile_idx_offset));
		} else if (json_tile_object instanceof JSONArray) {
			JSONArray json_variants = (JSONArray) json_tile_object;
			for (int variant_idx = 0; variant_idx < json_variants.size(); variant_idx++) {
				Object json_variant_object = json_variants.get(variant_idx);
				if (json_variant_object instanceof Long) {
					variants.add(
							new TileVariant((int) (long) json_variant_object, tile_idx_offset));
				} else if (json_variant_object instanceof JSONObject) {
					JSONObject json_variant = (JSONObject) json_variants.get(variant_idx);
					Object json_sprite_object = json_variant.get("sprite");
					if (json_sprite_object instanceof Long) {
						variants.add(
								new TileVariant((int) (long) json_sprite_object, tile_idx_offset));
					} else if (json_sprite_object instanceof JSONArray) {
						JSONArray json_sprites = (JSONArray) json_sprite_object;
						for (int sprite_idx = 0; sprite_idx < json_sprites.size(); sprite_idx++) {
							variants.add(new TileVariant((int) (long) json_sprites.get(sprite_idx),
									tile_idx_offset));
						}
					} else {
						throw new Exception("Unknown sprite format:" + json_sprite_object);
					}

				} else {
					throw new Exception("Unknown tile variant format:" + json_variant_object);
				}
			}
		} else {
			throw new Exception("Unsupported tile variant type:" + json_tile_object.toString());
		}
	}

	void registerTile(Object id, Tile tile, HashMap<String, Tile> dst_tiles) throws Exception {
		if (id instanceof String) {
			dst_tiles.put((String) id, tile);
		} else if (id instanceof JSONArray) {
			JSONArray ids = (JSONArray) id;
			for (int id_idx = 0; id_idx < ids.size(); id_idx++) {
				String real_id = (String) ids.get(id_idx);
				dst_tiles.put(real_id, tile);
			}
		} else {
			throw new Exception("Non supported value for tile id:" + id.toString());
		}
	}

	public boolean appendTilesFromPaletteItem(ArrayList<Tiles.RenderTile> dst, Palette.Item cell,
			JSONArray json_row, int row, int col, Palette palette, RenderFilter.eLayer layer) {
		String cell_type;

		if (cell.chance != 1 && Resources.random.nextInt(cell.chance) != 0) {
			return false;
		}
		if (!cell.entries.isEmpty()) {
			cell_type = cell.entries.get(Resources.random.nextInt(cell.entries.size()));
		} else {
			cell_type = "unknown";
		}
		appendTilesFromSymbol(dst, cell_type, json_row, row, col, false, palette, layer);
		return true;
	}

	public void appendTilesFromSymbol(ArrayList<Tiles.RenderTile> dst, String symbol,
			JSONArray json_row, int row, int col, boolean candidate_are_random, Palette palette,
			RenderFilter.eLayer layer) {
		Tile tile = tiles.get(symbol);
		if (tile == null || (tile.background.isEmpty() && tile.foreground.isEmpty())) {
			Tiles.RenderTile rendered_tile = new Tiles.RenderTile();
			dst.add(rendered_tile);
			rendered_tile.rotation = -1;
			rendered_tile.symbol = symbol;
			rendered_tile.tile = tile;
			rendered_tile.sprite = -1;
			rendered_tile.label_color = layer.color;//candidate_are_random ? Color.CYAN : Color.ORANGE;
			rendered_tile.candidate_are_random = candidate_are_random;
			rendered_tile.layer = layer;
			return;
		}
		// Background.
		if (!tile.background.isEmpty()) {
			drawVariant(dst, json_row, row, col, tile, tile.background.get(0), false,
					symbol + " (bg)", candidate_are_random, palette,
					RenderFilter.getSubTerrainLayer(layer, false));
		}
		// Foreground.
		if (!tile.foreground.isEmpty()) {
			drawVariant(dst, json_row, row, col, tile, tile.foreground.get(0), true,
					symbol + " (fg)", candidate_are_random, palette,
					RenderFilter.getSubTerrainLayer(layer, true));
		}
	}

	private void drawVariant(ArrayList<Tiles.RenderTile> dst, JSONArray json_row, int row, int col,
			Tile tile, TileVariant tile_variant, boolean is_foreground, String symbol,
			boolean candidate_are_random, Palette palette, RenderFilter.eLayer layer) {
		Tiles.RenderTile rendered_tile = new Tiles.RenderTile();
		dst.add(rendered_tile);
		rendered_tile.symbol = symbol;
		rendered_tile.layer = layer;
		rendered_tile.candidate_are_random = candidate_are_random;

		if (tile.multitile) {
			// "rotation" i.e. smooth connection to neighbor tiles of the same type.
			int neighbor_is_similar = getNeighborIsSimilarMask(json_row, row, col, palette);
			RotationAndTileOffset transformation = neighborIsSimilarToRotatedTileOffset(
					neighbor_is_similar, tile.rotates);

			boolean found_multi_tiles = false;
			TileVariant alternative_variant = null;
			Tile alternative_tile = tile.additional_tiles.get(transformation.alternative_key);
			if (alternative_tile != null) {
				if (is_foreground && !alternative_tile.foreground.isEmpty()) {
					int n = alternative_tile.foreground.size();
					if (n > 1) {
						found_multi_tiles = true;
					}
					alternative_variant = alternative_tile.foreground
							.get(transformation.rotation % n);
				}
				if (!is_foreground && !alternative_tile.background.isEmpty()) {
					int n = alternative_tile.background.size();
					if (n > 1) {
						found_multi_tiles = true;
					}
					alternative_variant = alternative_tile.background
							.get(transformation.rotation % n);
				}
			}

			if ((tile.rotates || !found_multi_tiles) && !Resources.tiles.iso) {
				rendered_tile.rotation = transformation.rotation;
				if (!found_multi_tiles && !tile.rotates) {
					if (rendered_tile.rotation == 1) {
						rendered_tile.rotation = 3;
					} else if (rendered_tile.rotation == 3) {
						rendered_tile.rotation = 1;
					}
				}
			}

			if (alternative_tile == null || alternative_variant == null) {
				alternative_tile = tile;
				alternative_variant = tile_variant;
			}
			rendered_tile.tile = alternative_tile;
			rendered_tile.sprite = alternative_variant.sprite;
		} else {
			rendered_tile.rotation = -1;
			rendered_tile.tile = tile;
			rendered_tile.sprite = tile_variant.sprite;
		}
	}

	int spriteToXSrc(int sprite, Tile tile) {
		return (sprite % tile.num_tile_x) * tile.tile_size_x_px;
	}

	int spriteToYSrc(int sprite, Tile tile) {
		return (sprite / tile.num_tile_x) * tile.tile_size_y_px;
	}

	public void render(Graphics2D dst, ArrayList<PrefabRendering.Label> labels,
			RenderTile render_tile, int x, int y, int w, int h, int col, int row) {

		int hwp = (w + 1) / 2;
		int hhp = (h + 1) / 2;

		if (render_tile.sprite == -1) {
			if (render_tile.symbol != null) {
				FontMetrics font_metric = dst.getFontMetrics();
				labels.add(new PrefabRendering.Label(render_tile.symbol,
						x + hwp - font_metric.stringWidth(render_tile.symbol) / 2,
						y + hhp - font_metric.getHeight() / 2 + 8, col, row, render_tile.layer));
			}
			return;
		}

		if (render_tile.tile.tile_size_x_px != base_size_x) {
			w = w * render_tile.tile.tile_size_x_px / base_size_x;
		}

		if (render_tile.tile.tile_size_y_px != base_size_y) {
			h = h * render_tile.tile.tile_size_y_px / base_size_y;
		}

		int hw = (w + 1) / 2;
		int hh = (h + 1) / 2;

		int nx = x - (render_tile.tile.tile_size_x_px - base_size_x + render_tile.tile.offset_x_px)
				* w / (2 * base_size_x);
		int ny = y - (render_tile.tile.tile_size_y_px - base_size_y) * w / (2 * base_size_y);

		int src_x = spriteToXSrc(render_tile.sprite, render_tile.tile);
		int src_y = spriteToYSrc(render_tile.sprite, render_tile.tile);
		if (render_tile.rotation != -1) {
			AffineTransform old = dst.getTransform();
			dst.translate(x + hw, y + hh);
			dst.rotate(Math.toRadians(render_tile.rotation * 90));
			dst.drawImage(render_tile.tile.image, -hw, -hh, hw, hh, src_x, src_y,
					src_x + render_tile.tile.tile_size_x_px,
					src_y + render_tile.tile.tile_size_y_px, null);
			dst.setTransform(old);
		} else {
			dst.drawImage(render_tile.tile.image, nx, ny, nx + w, ny + h, src_x, src_y,
					src_x + render_tile.tile.tile_size_x_px,
					src_y + render_tile.tile.tile_size_y_px, null);
		}

		if (render_tile.candidate_are_random) {
			dst.setColor(Color.CYAN);
			dst.drawRect(x, y, w - 1, h - 1);
		}

	}

	int getNeighborIsSimilarMask(JSONArray json_row, int row, int col, Palette palette) {
		int neighbor_map = 0;
		char cur_character = ((String) json_row.get(row)).charAt(col);
		ArrayList<Item> palette_cell_set = palette.char_to_id.get(cur_character);
		if (palette_cell_set == null) {
			return 0;
		}
		for (int dir_idx = 0; dir_idx < Vector2i.DIRS4.length; dir_idx++) {
			Vector2i dir = Vector2i.DIRS4[dir_idx];
			int nei_col = col + dir.x;
			int nei_row = row + dir.y;
			if (nei_row < 0 || nei_row >= json_row.size()) {
				continue;
			}
			String nei_col_data = (String) json_row.get(nei_row);
			if (nei_col < 0 || nei_col >= nei_col_data.length()) {
				continue;
			}
			char nei_character = ((String) json_row.get(nei_row)).charAt(nei_col);
			ArrayList<Item> nei_palette_cell_set = palette.char_to_id.get(nei_character);
			if (nei_palette_cell_set != null
					&& hasCommonSymbol(nei_palette_cell_set, palette_cell_set)) {
				neighbor_map |= 1 << dir_idx;
			}
		}
		return neighbor_map;
	}

	private boolean hasCommonSymbol(ArrayList<Item> a, ArrayList<Item> b) {
		for (Item a_i : a) {
			for (String a_s : a_i.entries) {
				for (Item b_i : b) {
					for (String b_s : b_i.entries) {
						if (a_s.startsWith(b_s)) {
							return true;
						}
					}
				}

			}
		}
		return false;
	}

	RotationAndTileOffset[] buildNeighborMapToRotationRotate() {
		RotationAndTileOffset[] map = new RotationAndTileOffset[16];
		map[0b1111] = new RotationAndTileOffset(0, "center");

		map[0b0011] = new RotationAndTileOffset(0, "corner");
		map[0b0110] = new RotationAndTileOffset(1, "corner");
		map[0b1100] = new RotationAndTileOffset(2, "corner");
		map[0b1001] = new RotationAndTileOffset(3, "corner");

		map[0b0101] = new RotationAndTileOffset(1, "edge");
		map[0b1010] = new RotationAndTileOffset(0, "edge");

		map[0b0001] = new RotationAndTileOffset(3, "end_piece");
		map[0b0010] = new RotationAndTileOffset(0, "end_piece");
		map[0b0100] = new RotationAndTileOffset(1, "end_piece");
		map[0b1000] = new RotationAndTileOffset(2, "end_piece");

		map[0b0111] = new RotationAndTileOffset(0, "t_connection");
		map[0b1110] = new RotationAndTileOffset(1, "t_connection");
		map[0b1101] = new RotationAndTileOffset(2, "t_connection");
		map[0b1011] = new RotationAndTileOffset(3, "t_connection");

		map[0b0000] = new RotationAndTileOffset(0, "unconnected");

		return map;
	}

	RotationAndTileOffset[] buildNeighborMapToRotationNonRotate() {
		RotationAndTileOffset[] map = new RotationAndTileOffset[16];
		map[0b1111] = new RotationAndTileOffset(0, "center");

		map[0b0011] = new RotationAndTileOffset(0, "corner");
		map[0b0110] = new RotationAndTileOffset(3, "corner");
		map[0b1100] = new RotationAndTileOffset(2, "corner");
		map[0b1001] = new RotationAndTileOffset(1, "corner");

		map[0b0101] = new RotationAndTileOffset(1, "edge");
		map[0b1010] = new RotationAndTileOffset(0, "edge");

		map[0b0001] = new RotationAndTileOffset(3, "end_piece");
		map[0b0010] = new RotationAndTileOffset(0, "end_piece");
		map[0b0100] = new RotationAndTileOffset(1, "end_piece");
		map[0b1000] = new RotationAndTileOffset(2, "end_piece");

		map[0b0111] = new RotationAndTileOffset(0, "t_connection");
		map[0b1110] = new RotationAndTileOffset(1, "t_connection");
		map[0b1101] = new RotationAndTileOffset(2, "t_connection");
		map[0b1011] = new RotationAndTileOffset(3, "t_connection");

		map[0b0000] = new RotationAndTileOffset(0, "unconnected");

		return map;
	}

	RotationAndTileOffset neighborIsSimilarToRotatedTileOffset(int neighbor_is_similar,
			boolean rotate) {
		neighbor_map_to_rotation_rotate = null;
		neighbor_map_to_rotation_non_rotate = null;
		if (neighbor_map_to_rotation_rotate == null) {
			neighbor_map_to_rotation_rotate = buildNeighborMapToRotationRotate();
		}

		if (neighbor_map_to_rotation_non_rotate == null) {
			neighbor_map_to_rotation_non_rotate = buildNeighborMapToRotationNonRotate();
		}
		if (rotate) {
			return neighbor_map_to_rotation_rotate[neighbor_is_similar];
		} else {
			return neighbor_map_to_rotation_non_rotate[neighbor_is_similar];
		}
	}

}
