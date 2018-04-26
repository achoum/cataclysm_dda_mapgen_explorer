
package cataclysm_dda_prefab_explorer.component;

import cataclysm_dda_prefab_explorer.content_index.ContentIndex;
import cataclysm_dda_prefab_explorer.render.PrefabRendering;
import cataclysm_dda_prefab_explorer.resources_loader.Palette;
import cataclysm_dda_prefab_explorer.utils.Vector2i;

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

	// The current prefab (stored as a json object).
	JSONObject json_prefab = null;
	// The index of the prefab.
	ContentIndex.Prefab prefab = null;
	// The rendering buffer for the prefab.
	BufferedImage prefab_buffer = null;
	// The rendering buffer for the overlay.
	BufferedImage overlay_buffer = null;
	// The palette of symbols used by the prefab.
	Palette palette = null;
	// The zoom.
	float cell_drawing_size_in_px = 32.f;

	// Last known position of the mouse. Used for the info box.
	int last_mouse_x = -1;
	int last_mouse_y = -1;
	// The object ready to be rendered.
	PrefabRendering rendering;

	public PrefabRenderPanel() {
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addComponentListener(this);
		setBackground(new Color(0, 0, 0));
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		last_mouse_x = evt.getX();
		last_mouse_y = evt.getY();
		refresh();
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		// Setup the rendering to use all the available room.
		int w_max_cell_size = getWidth() / rendering.num_cols;
		int h_max_cell_size = getHeight() / rendering.num_rows;
		cell_drawing_size_in_px = Math.min(w_max_cell_size, h_max_cell_size);
		revalidate();
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (rendering == null) {
			g.setColor(new Color(255, 255, 255));
			g.drawString("No prefab selected.", 10, 16);
			return;
		}
		int drawing_width = (int) (rendering.num_cols * cell_drawing_size_in_px);
		int drawing_height = (int) (rendering.num_rows * cell_drawing_size_in_px);
		ensureDrawingBufferAreReady(drawing_width, drawing_height);
		renderOverlay();
		g.drawImage(prefab_buffer, 0, 0, drawing_width, drawing_height, null);
		g.drawImage(overlay_buffer, 0, 0, getWidth(), getHeight(), null);
	}

	void renderOverlay() {
		Graphics2D g = (Graphics2D) overlay_buffer.getGraphics();
		g.setBackground(new Color(255, 255, 255, 0));
		g.clearRect(0, 0, overlay_buffer.getWidth(), overlay_buffer.getHeight());

		Vector2i selected_cell = renderToCellCoordinates(last_mouse_x, last_mouse_y);
		if (selected_cell != null) {
			ArrayList<String> information = new ArrayList<>();
			rendering.appendCellDescription(selected_cell.x, selected_cell.y, information);
			renderInfo(g, overlay_buffer, last_mouse_x, last_mouse_y, information);
		}
	}

	// The text in the infobox when putting the mouse over a cell.
	private void renderInfo(Graphics2D g, BufferedImage buffer, int x, int y,
			ArrayList<String> information) {
		x += 5;
		y += 5;
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
		g.fillRect(x, y, max_width + margin * 2, y_spacing * information.size() + margin * 2);
		g.setColor(new Color(0, 0, 0));
		g.drawRect(x, y, max_width + margin * 2, y_spacing * information.size() + margin * 2);
		for (int information_idx = 0; information_idx < information.size(); information_idx++) {
			String info = information.get(information_idx);
			g.drawString(info, x + margin, y + (information_idx + 1) * y_spacing);
		}
	}

	// Project the mouse coordinate to a prefab cell coordinate.
	private Vector2i renderToCellCoordinates(int x, int y) {
		int cell_x = (int) (Math.floor(x / cell_drawing_size_in_px));
		int cell_y = (int) (Math.floor(y / cell_drawing_size_in_px));
		if (cell_x < 0 || cell_x >= rendering.num_cols || cell_y < 0
				|| cell_y >= rendering.num_rows) {
			return null;
		}
		return new Vector2i(cell_x, cell_y);
	}

	void ensureDrawingBufferAreReady(int width, int height) {
		if (prefab_buffer == null || prefab_buffer.getWidth() != width
				|| prefab_buffer.getHeight() != height) {
			prefab_buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			rendering.render((Graphics2D) prefab_buffer.getGraphics(), 0, 0, width, height);
		}
		if (overlay_buffer == null || overlay_buffer.getWidth() != getWidth()
				|| overlay_buffer.getHeight() != getHeight()) {
			overlay_buffer = new BufferedImage(getWidth(), getHeight(),
					BufferedImage.TYPE_INT_ARGB);
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
		refresh();
	}

	public void loadPrefab(String main_directory, ContentIndex.Prefab prefab) throws Exception {
		this.prefab = prefab;
		this.prefab_buffer = null;
		System.out.printf("Loading prefab %d in %s\n", prefab.index, prefab.file.getAbsolutePath());
		JSONParser parser = new JSONParser();
		FileReader reader = new FileReader(prefab.file);
		Object content = parser.parse(reader);
		if (!(content instanceof JSONArray))
			throw new Exception("Not a prefab");
		JSONArray content_array = (JSONArray) content;
		Object item = content_array.get(prefab.index);
		if (!(item instanceof JSONObject))
			throw new Exception("Not a prefab");
		json_prefab = (JSONObject) item;
		reader.close();
		palette = new Palette();
		palette.load(main_directory, json_prefab);
		rendering = new PrefabRendering(json_prefab, palette);
		rendering.update();
		refresh();
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

	public void refresh() {
		revalidate();
		repaint();
	}

}
