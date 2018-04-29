
package mapgen_explorer.component;

import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.render.PrefabRendering;
import mapgen_explorer.resources_loader.Palette;
import mapgen_explorer.utils.Vector2i;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.util.ArrayList;

// Render of a prefab.
public class PrefabRenderPanel extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener, ComponentListener {

	// The current prefab json object.
	public JSONObject json_prefab = null;
	// The index of the prefab in the json file (one file can contain several prefabs).
	public ContentIndex.Prefab prefab = null;
	// Buffer for the rendering of the prefab.
	BufferedImage prefab_buffer = null;
	// Buffer for the rendering of the overlays (e.g. the infobox).
	BufferedImage overlay_buffer = null;
	// The palette of symbols used by the prefab. Assembled from the symbol defined in the prefab and the symbol defined in the external palettes.
	public Palette palette = null;
	// The zoom. Size of a cell in pixel.
	int cell_drawing_size_in_px = 32;
	// Drawing origin of the prefab. For "moving around".
	int render_origin_x = 0;
	int render_origin_y = 0;
	// The current active mouse button.
	int active_button = -1;
	// If true, the prefab "prefab_buffer" will be recomputed at the next drawing event.
	boolean force_re_render = false;
	// Callback on the mouse actions. Used by the editor. If not specified, all the mouse buttons are "moving the camera around".
	ActionCallBack action_callback = null;
	// Last position send to the "action_callback" (if any).
	Vector2i last_action_callback_cell_position = new Vector2i(-1, -1);
	// Last known position of the mouse.
	int last_mouse_x = -1;
	int last_mouse_y = -1;
	// The pre-rendering structure of the prefab.
	public PrefabRendering rendering;

	public interface ActionCallBack {

		void action(Vector2i cell_coordinate);
	}

	public PrefabRenderPanel() {
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addComponentListener(this);
		setBackground(new Color(0, 0, 0));
	}

	public void setActionCallBack(ActionCallBack action_callback) {
		this.action_callback = action_callback;
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		last_mouse_x = evt.getX();
		last_mouse_y = evt.getY();
		askRedraw();
	}

	// Send an action to the callback (i.e. the editor).
	void sendActionCallBack() {
		Vector2i cell_coordinates = screenToCellCoordinates(last_mouse_x - render_origin_x,
				last_mouse_y - render_origin_y);
		if (cell_coordinates != null
				&& !last_action_callback_cell_position.equals(cell_coordinates)) {
			last_action_callback_cell_position.set(cell_coordinates);
			action_callback.action(cell_coordinates);
		}
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		last_mouse_x = evt.getX();
		last_mouse_y = evt.getY();
		active_button = evt.getButton();
		if (action_callback != null && active_button == MouseEvent.BUTTON1) {
			// Action.
			sendActionCallBack();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int dx = e.getX() - last_mouse_x;
		int dy = e.getY() - last_mouse_y;
		last_mouse_x = e.getX();
		last_mouse_y = e.getY();
		if (action_callback == null || active_button != MouseEvent.BUTTON1) {
			// Moving around.
			render_origin_x += dx;
			render_origin_y += dy;
		} else {
			// Action.
			sendActionCallBack();
		}
		askRedraw();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	// In/out zoom with the mouse wheel.
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		float scale = 1.f;
		float speed = 1.1f;
		if (e.getWheelRotation() > 0) {
			scale /= speed;
		} else if (e.getWheelRotation() < 0) {
			scale *= speed;
		}
		int save_cell_drawing_size_in_px = cell_drawing_size_in_px;
		cell_drawing_size_in_px *= scale;
		if (cell_drawing_size_in_px == save_cell_drawing_size_in_px) {
			cell_drawing_size_in_px++;
		}
		if (cell_drawing_size_in_px > 128) {
			cell_drawing_size_in_px = 128;
		} else if (cell_drawing_size_in_px < 4) {
			cell_drawing_size_in_px = 4;
		} else {
			render_origin_x += (last_mouse_x - render_origin_x) * (1 - scale);
			render_origin_y += (last_mouse_y - render_origin_y) * (1 - scale);
		}
		askRedraw();
	}

	// The component is resized.
	@Override
	public void componentResized(ComponentEvent e) {
		resetZoomToShowAll();
	}

	// Setup the rendering to use all the available room.
	public void resetZoomToShowAll() {
		if (rendering == null) {
			return;
		}
		int w_max_cell_size = getWidth() / rendering.num_cols;
		int h_max_cell_size = getHeight() / rendering.num_rows;
		cell_drawing_size_in_px = Math.min(w_max_cell_size, h_max_cell_size);
		if (cell_drawing_size_in_px < 4) {
			cell_drawing_size_in_px = 4;
		}
		render_origin_x = 0;
		render_origin_y = 0;
		askRedraw();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (rendering == null) {
			g.setColor(new Color(255, 255, 255));
			g.drawString("No prefab selected.", 10, 16);
			return;
		}
		int prefab_rendering_width = rendering.num_cols * cell_drawing_size_in_px;
		int prefab_rendering_height = rendering.num_rows * cell_drawing_size_in_px;
		try {
			renderPrefabIfNecacary(prefab_rendering_width, prefab_rendering_height);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			renderOverlay();
		} catch (Exception e) {
			e.printStackTrace();
		}
		g.drawImage(prefab_buffer, render_origin_x, render_origin_y, prefab_rendering_width,
				prefab_rendering_height, null);
		g.drawImage(overlay_buffer, 0, 0, getWidth(), getHeight(), null);
	}

	void renderOverlay() throws Exception {
		if (overlay_buffer == null || overlay_buffer.getWidth() != getWidth()
				|| overlay_buffer.getHeight() != getHeight()) {
			// The size of the rendering target buffer has changed.
			overlay_buffer = new BufferedImage(getWidth(), getHeight(),
					BufferedImage.TYPE_INT_ARGB);
		}

		Graphics2D g = (Graphics2D) overlay_buffer.getGraphics();
		g.setBackground(new Color(255, 255, 255, 0));
		g.clearRect(0, 0, overlay_buffer.getWidth(), overlay_buffer.getHeight());
		Vector2i selected_cell_coordinates = screenToCellCoordinates(last_mouse_x - render_origin_x,
				last_mouse_y - render_origin_y);
		if (selected_cell_coordinates != null) {
			// Highlight of the cell under the mouse.
			Vector2i cell_screen_coordinates = cellToScreeCoordinates(selected_cell_coordinates.x,
					selected_cell_coordinates.y);
			g.setColor(new Color(0, 255, 0));
			g.drawRect(cell_screen_coordinates.x, cell_screen_coordinates.y,
					cell_drawing_size_in_px, cell_drawing_size_in_px);
			// Information box.
			ArrayList<String> information = new ArrayList<>();
			rendering.appendCellDescription(selected_cell_coordinates.x,
					selected_cell_coordinates.y, information);
			renderInfo(g, overlay_buffer, last_mouse_x, last_mouse_y, information);
		}
	}

	// The text in the infobox when putting the mouse over a cell.
	private void renderInfo(Graphics2D g, BufferedImage buffer, int screen_pos_x, int screen_pos_y,
			ArrayList<String> information) {
		screen_pos_x += 5;
		screen_pos_y += 5;
		int margin = 5;
		int max_width = 1;
		int y_spacing = g.getFontMetrics().getHeight();
		for (int information_idx = 0; information_idx < information.size(); information_idx++) {
			String info = information.get(information_idx);
			int length = g.getFontMetrics().stringWidth(info);
			if (length > max_width) {
				max_width = length;
			}
		}
		g.setColor(new Color(255, 255, 255, 200));
		g.fillRect(screen_pos_x, screen_pos_y, max_width + margin * 2,
				y_spacing * information.size() + margin * 2);
		g.setColor(new Color(0, 0, 0));
		g.drawRect(screen_pos_x, screen_pos_y, max_width + margin * 2,
				y_spacing * information.size() + margin * 2);
		for (int information_idx = 0; information_idx < information.size(); information_idx++) {
			String info = information.get(information_idx);
			g.drawString(info, screen_pos_x + margin,
					screen_pos_y + (information_idx + 1) * y_spacing);
		}
	}

	// Project the mouse coordinate to a prefab cell coordinate.
	private Vector2i screenToCellCoordinates(int x, int y) {
		int cell_x = Math.floorDiv(x, cell_drawing_size_in_px);
		int cell_y = Math.floorDiv(y, cell_drawing_size_in_px);
		if (cell_x < 0 || cell_x >= rendering.num_cols || cell_y < 0
				|| cell_y >= rendering.num_rows) {
			return null;
		}
		return new Vector2i(cell_x, cell_y);
	}

	private Vector2i cellToScreeCoordinates(int x, int y) {
		int cell_x = x * cell_drawing_size_in_px + render_origin_x;
		int cell_y = y * cell_drawing_size_in_px + render_origin_y;
		return new Vector2i(cell_x, cell_y);
	}

	void renderPrefabIfNecacary(int width, int height) throws Exception {
		if (prefab_buffer == null || prefab_buffer.getWidth() != width
				|| prefab_buffer.getHeight() != height) {
			// The size of the target rendering area has changed.
			prefab_buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			force_re_render = true;
		}
		if (force_re_render) {
			force_re_render = false;
			Graphics2D g2d = (Graphics2D) prefab_buffer.getGraphics();
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, prefab_buffer.getWidth(), prefab_buffer.getHeight());
			rendering.render(g2d, 0, 0, width, height);
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		last_mouse_x = last_mouse_y = -1;
		askRedraw();
	}

	// Load a prefab. Set the zoom to show everything.
	public void loadPrefab(String main_directory, ContentIndex.Prefab prefab) throws Exception {
		this.prefab = prefab;
		this.prefab_buffer = null;
		System.out.printf("Loading prefab %d in %s\n", prefab.index, prefab.file.getAbsolutePath());
		JSONParser parser = new JSONParser();
		FileReader reader = new FileReader(prefab.file);
		Object content = parser.parse(reader);
		reader.close();
		finalizeLoading(main_directory, content);
		resetZoomToShowAll();
	}

	// Load a prefab. Instead of getting the content of the file described by "prefab_index" use
	// "prefab_content" as replacement (interpreted as raw json). Unlike the version above,
	// don't set the zoom to show everything.
	public void loadPrefab(String main_directory, ContentIndex.Prefab prefab_index,
			String override_input_prefab_content) throws Exception {
		this.prefab = prefab_index;
		this.prefab_buffer = null;
		System.out.printf("Loading prefab %d in %s\n", prefab.index, prefab.file.getAbsolutePath());
		JSONParser parser = new JSONParser();
		Object content;
		if (override_input_prefab_content == null) {
			FileReader reader = new FileReader(prefab.file);
			content = parser.parse(reader);
			reader.close();
		} else {
			content = parser.parse(override_input_prefab_content);
		}
		finalizeLoading(main_directory, content);
		askRedraw();
	}

	// Finalize the loading a new prefab. Check that the top level of the json is well formatted,
	// build the palette, and the renderer. Is called at the end of "loadPrefab".
	void finalizeLoading(String main_directory, Object content) throws Exception {
		if (!(content instanceof JSONArray))
			throw new Exception("Not a prefab");
		JSONArray content_array = (JSONArray) content;
		Object item = content_array.get(prefab.index);
		if (!(item instanceof JSONObject))
			throw new Exception("Not a prefab");
		json_prefab = (JSONObject) item;
		palette = new Palette();
		palette.loadFromPrefab(main_directory, json_prefab);
		palette.loadFromJsonContentArray(content_array);
		rendering = new PrefabRendering(json_prefab, palette);
		rendering.update();
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}

	// Re-draw the prefab buffer after recomputing it from the renderer structure.
	public void askUpdateRenderAndRedraw() {
		force_re_render = true;
		askRedraw();
	}

	// Re-draw the prefab buffer. Don't recompute it.
	public void askRedraw() {
		revalidate();
		repaint();
	}

}
