
package cataclysm_dda_prefab_explorer.resources_loader;

import cataclysm_dda_prefab_explorer.utils.Files;
import cataclysm_dda_prefab_explorer.utils.Vector2i;
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

	HashMap<String, Tile> tiles = new HashMap<String, Tile>();

	public static class TileVariant {
		public TileVariant(int sprite) {
			this.sprite = sprite;
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

	public static RotationAndTileOffset[] neighbor_map_to_rotation;

	public static class Tile {
		public BufferedImage image;
		public ArrayList<TileVariant> foreground = new ArrayList<>();
		public ArrayList<TileVariant> background = new ArrayList<>();
		public HashMap<String, Tile> additional_tiles = new HashMap<>();
		public int tile_size_x_px, tile_size_y_px;
		public int num_tile_x, num_tile_y;
		public boolean rotates;
		public boolean multitile;
	}

	public static class RenderTile {
		public int rotation = 0;
		public int sprite = -1;
		public String symbol;
		public Tile tile;
	}

	String findBaseTileImageFilename(String tile_directory) throws Exception {
		File[] candidates = (new File(tile_directory)).listFiles();
		for (File candidate : candidates) {
			if (Files.getExtension(candidate.getName()).equals("png")) {
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
		int base_size_x = (int) (long) tile_info.get("width");
		int base_size_y = (int) (long) tile_info.get("height");
		JSONArray json_tiles = (JSONArray) json_tile.get("tiles");
		if (json_tiles != null) {
			loadTiles(json_tiles, Paths.get(tile_directory, base_tile_filename).toString(),
					base_size_x, base_size_y);
		}
		JSONArray json_new_tiles = (JSONArray) json_tile.get("tiles-new");
		if (json_new_tiles != null) {
			for (int new_tile_idx = 0; new_tile_idx < json_new_tiles.size(); new_tile_idx++) {
				JSONObject new_tile = (JSONObject) (json_new_tiles.get(new_tile_idx));
				JSONArray sub_tiles = (JSONArray) new_tile.get("tiles");
				int size_x = (int) (long) new_tile.getOrDefault("sprite_width", (long) base_size_x);
				int size_y = (int) (long) new_tile.getOrDefault("sprite_height",
						(long) base_size_y);
				loadTiles(sub_tiles,
						Paths.get(tile_directory, (String) new_tile.get("file")).toString(), size_x,
						size_y);
			}
		}
	}

	void loadTiles(JSONArray json_tiles, String file_image_path, int tile_size_x, int tile_size_y)
			throws Exception {
		FileInputStream image_reader = new FileInputStream(new File(file_image_path));
		BufferedImage image = ImageIO.read(new BufferedInputStream(image_reader));
		loadTiles(json_tiles, image, tile_size_x, tile_size_y, tiles);
	}

	void loadTiles(JSONArray json_tiles, BufferedImage image, int tile_size_x, int tile_size_y,
			HashMap<String, Tile> dst_tiles) throws Exception {
		int num_split_columns = image.getWidth() / tile_size_x;
		for (int tile_idx = 0; tile_idx < json_tiles.size(); tile_idx++) {
			JSONObject json_tile = (JSONObject) json_tiles.get(tile_idx);
			Object tile_id = json_tile.get("id");
			Tile tile = new Tile();
			tile.image = image;
			Object json_foreground = json_tile.get("fg");
			if (json_foreground != null) {
				loadTile(json_foreground, num_split_columns, tile_size_x, tile_size_y,
						tile.foreground);
			}
			Object json_background = json_tile.get("bg");
			if (json_background != null) {
				loadTile(json_background, num_split_columns, tile_size_x, tile_size_y,
						tile.background);
			}

			tile.rotates = (Boolean) json_tile.getOrDefault("rotates", Boolean.FALSE);
			tile.multitile = (Boolean) json_tile.getOrDefault("multitile", Boolean.FALSE);
			JSONArray json_additional_tiles = (JSONArray) json_tile.get("additional_tiles");
			if (json_additional_tiles != null) {
				loadTiles(json_additional_tiles, image, tile_size_x, tile_size_y,
						tile.additional_tiles);
			}

			tile.tile_size_x_px = tile_size_x;
			tile.tile_size_y_px = tile_size_y;
			tile.num_tile_x = image.getWidth() / tile_size_x;
			tile.num_tile_y = image.getHeight() / tile_size_y;
			;
			registerTile(tile_id, tile, dst_tiles);
		}
	}

	private void loadTile(Object json_tile_object, int num_split_columns, int tile_size_x,
			int tile_size_y, ArrayList<TileVariant> variants) throws Exception {
		if (json_tile_object instanceof Long) {
			variants.add(new TileVariant((int) (long) json_tile_object));
		} else if (json_tile_object instanceof JSONArray) {
			JSONArray json_variants = (JSONArray) json_tile_object;
			for (int variant_idx = 0; variant_idx < json_variants.size(); variant_idx++) {
				Object json_variant_object = json_variants.get(variant_idx);
				if (json_variant_object instanceof Long) {
					variants.add(new TileVariant((int) (long) json_variant_object));
				} else if (json_variant_object instanceof JSONObject) {
					JSONObject json_variant = (JSONObject) json_variants.get(variant_idx);
					Object json_sprite_object = json_variant.get("sprite");
					if (json_sprite_object instanceof Long) {
						variants.add(new TileVariant((int) (long) json_sprite_object));
					} else if (json_sprite_object instanceof JSONArray) {
						JSONArray json_sprites = (JSONArray) json_sprite_object;
						for (int sprite_idx = 0; sprite_idx < json_sprites.size(); sprite_idx++) {
							variants.add(
									new TileVariant((int) (long) json_sprites.get(sprite_idx)));
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

	public void appendTiles(ArrayList<Tiles.RenderTile> dst, Palette.Cell cell, JSONArray json_row,
			int row, int col) {
		String cell_type = cell.entries.get(Resources.random.nextInt(cell.entries.size()));
		appendTiles(dst, cell_type, json_row, row, col);
	}

	public void appendTiles(ArrayList<Tiles.RenderTile> dst, String symbol, JSONArray json_row,
			int row, int col) {
		Tile tile = tiles.get(symbol);
		if (tile == null) {
			Tiles.RenderTile rendered_tile = new Tiles.RenderTile();
			dst.add(rendered_tile);
			rendered_tile.rotation = -1;
			rendered_tile.symbol = symbol;
			rendered_tile.tile = tile;
			rendered_tile.sprite = -1;
			return;
		}
		// Background.
		if (!tile.background.isEmpty()) {
			drawVariant(dst, json_row, row, col, tile, tile.background.get(0), false,
					symbol + " (bg)");
		}
		// Foreground.
		if (!tile.foreground.isEmpty()) {
			drawVariant(dst, json_row, row, col, tile, tile.foreground.get(0), true,
					symbol + " (fg)");
		}
	}

	private void drawVariant(ArrayList<Tiles.RenderTile> dst, JSONArray json_row, int row, int col,
			Tile tile, TileVariant tile_variant, boolean is_foreground, String symbol) {
		if (tile.rotates) {
			Tiles.RenderTile rendered_tile = new Tiles.RenderTile();
			dst.add(rendered_tile);
			rendered_tile.symbol = symbol;
			// "rotation" i.e. smooth connection to neighbor tiles of the same type.
			int neighbor_is_similar = getNeighborIsSimilarMask(json_row, row, col);
			RotationAndTileOffset transformation = neighborIsSimilarToRotatedTileOffset(
					neighbor_is_similar);
			rendered_tile.rotation = transformation.rotation;
			TileVariant alternative_variant = null;
			Tile alternative_tile = tile.additional_tiles.get(transformation.alternative_key);
			if (alternative_tile != null) {
				if (is_foreground && !alternative_tile.foreground.isEmpty()) {
					alternative_variant = alternative_tile.foreground.get(0);
				}
				if (!is_foreground && !alternative_tile.background.isEmpty()) {
					alternative_variant = alternative_tile.background.get(0);
				}
			}
			if (alternative_tile == null || alternative_variant == null) {
				alternative_tile = tile;
				alternative_variant = tile_variant;
			}
			rendered_tile.tile = alternative_tile;
			rendered_tile.sprite = alternative_variant.sprite;
		} else {
			Tiles.RenderTile rendered_tile = new Tiles.RenderTile();
			dst.add(rendered_tile);
			rendered_tile.rotation = -1;
			rendered_tile.symbol = symbol;
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

	public void render(Graphics2D dst, RenderTile render_tile, int x, int y, int w, int h) {

		if (render_tile.sprite == -1) {
			if (render_tile.symbol != null) {
				FontMetrics font_metric = dst.getFontMetrics();
				dst.setColor(new Color(255, 0, 0));
				dst.drawString(render_tile.symbol,
						x + w / 2 - font_metric.stringWidth(render_tile.symbol) / 2,
						y + h / 2 - font_metric.getHeight() / 2 + 8);
			}
			return;
		}

		int src_x = spriteToXSrc(render_tile.sprite, render_tile.tile);
		int src_y = spriteToYSrc(render_tile.sprite, render_tile.tile);
		if (render_tile.rotation != -1) {
			AffineTransform old = dst.getTransform();
			dst.translate(x + w / 2, y + h / 2);
			dst.rotate(Math.toRadians(render_tile.rotation * 90));
			dst.drawImage(render_tile.tile.image, -w / 2, -h / 2, w / 2, h / 2, src_x, src_y,
					src_x + render_tile.tile.tile_size_x_px,
					src_y + render_tile.tile.tile_size_y_px, null);
			dst.setTransform(old);
		} else {
			dst.drawImage(render_tile.tile.image, x, y, x + w, y + h, src_x, src_y,
					src_x + render_tile.tile.tile_size_x_px,
					src_y + render_tile.tile.tile_size_y_px, null);
		}
	}

	int getNeighborIsSimilarMask(JSONArray json_row, int row, int col) {
		int neighbor_map = 0;
		char type = ((String) json_row.get(row)).charAt(col);
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
			char nei_type = ((String) json_row.get(nei_row)).charAt(nei_col);
			if (type == nei_type) {
				neighbor_map |= 1 << dir_idx;
			}
		}
		return neighbor_map;
	}

	RotationAndTileOffset[] buildNeighborMapToRotation() {
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

	RotationAndTileOffset neighborIsSimilarToRotatedTileOffset(int neighbor_is_similar) {
		if (neighbor_map_to_rotation == null) {
			neighbor_map_to_rotation = buildNeighborMapToRotation();
		}
		return neighbor_map_to_rotation[neighbor_is_similar];
	}

}
