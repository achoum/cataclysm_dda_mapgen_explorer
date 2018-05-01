
package mapgen_explorer.component;

import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.content_index.ContentIndex.Directory;
import mapgen_explorer.content_index.ContentIndex.JsonFile;
import mapgen_explorer.content_index.ContentIndex.Palette;
import mapgen_explorer.content_index.ContentIndex.Prefab;
import mapgen_explorer.resources_loader.Resources;
import mapgen_explorer.window.MapgenExplorer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

// Display in a JTree of the content (currently, only the mapgen json files are supported).
public class ContentIndexTree extends JTree implements MouseListener {

	// Index of the content.
	public ContentIndex content_index = new ContentIndex();
	public MapgenExplorer parent;

	// Selects the display icons.
	DefaultTreeCellRenderer node_render = new DefaultTreeCellRenderer() {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
				boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component default_render = super.getTreeCellRendererComponent(tree, value, selected,
					expanded, leaf, row, hasFocus);
			Object o = ((DefaultMutableTreeNode) value).getUserObject();
			if (o instanceof ContentIndex.Directory) {
				setIcon(Resources.icons.directory);
			} else if (o instanceof ContentIndex.JsonFile) {
				setIcon(Resources.icons.jsonfile);
			} else if (o instanceof ContentIndex.Prefab) {
				setIcon(Resources.icons.prefab);
			} else if (o instanceof ContentIndex.Palette) {
				setIcon(Resources.icons.palette);
			}
			return default_render;
		}
	};

	public ContentIndexTree(MapgenExplorer parent) {
		this.parent = parent;
		setCellRenderer(node_render);
		addMouseListener(this);
	}

	boolean matchFilter(String filter, String value) {
		if (filter.isEmpty())
			return true;
		return value.contains(filter);
	}

	// Fill the displayed tree with the directories in the indexed content.
	boolean recursiveBuildTree(ContentIndex.Directory directory, DefaultMutableTreeNode node,
			String filter, boolean force_match) {
		boolean match_found = force_match || matchFilter(filter, directory.file.getName());
		for (JsonFile json_file : directory.json_files) {
			DefaultMutableTreeNode json_file_node = new DefaultMutableTreeNode(json_file);
			if (recursiveBuildTree(json_file, json_file_node, filter, match_found)) {
				node.add(json_file_node);
				match_found = true;
			}
		}
		for (Directory sub_directory : directory.sub_directories) {
			DefaultMutableTreeNode sub_directory_node = new DefaultMutableTreeNode(sub_directory);
			if (recursiveBuildTree(sub_directory, sub_directory_node, filter, match_found)) {
				match_found = true;
				node.add(sub_directory_node);
			}
		}
		return match_found;
	}

	// Fill the displayed tree with the json files in the indexed content.
	boolean recursiveBuildTree(ContentIndex.JsonFile json_file, DefaultMutableTreeNode node,
			String filter, boolean force_match) {
		boolean match_found = force_match || matchFilter(filter, json_file.file.getName());
		for (Prefab prefab : json_file.prefabs) {
			if (match_found || matchFilter(filter, prefab.toString())) {
				DefaultMutableTreeNode prefab_node = new DefaultMutableTreeNode(prefab);
				node.add(prefab_node);
				match_found = true;
			}
		}

		for (Palette palette : json_file.palettes) {
			if (match_found || matchFilter(filter, palette.toString())) {
				DefaultMutableTreeNode palette_node = new DefaultMutableTreeNode(palette);
				node.add(palette_node);
				match_found = true;
			}
		}

		return match_found;
	}

	// Re-index the content and refresh the display.
	public void refresh(String main_directory) {
		content_index.update(main_directory);
		fillFromIndex("");
		expandAllNodes();
	}

	// Refresh the display with a filter.
	public void filter(String filter) {
		fillFromIndex(filter);
		expandAllNodes();
	}

	public void expandAllNodes() {
		recursiveExpandAllNodes(0, getRowCount());
	}

	public void collapseAllNodes() {
		recursiveCollapseAllNodes(new TreePath(getModel().getRoot()), 0);
	}

	// Fill the displayed tree with the indexed content.
	void fillFromIndex(String filter) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(content_index.root);
		recursiveBuildTree(content_index.root, root, filter, false);
		DefaultTreeModel model = new DefaultTreeModel(root);
		setModel(model);
	}

	void recursiveExpandAllNodes(int startingIndex, int rowCount) {
		for (int i = startingIndex; i < rowCount; ++i) {
			expandRow(i);
		}
		if (getRowCount() != rowCount) {
			recursiveExpandAllNodes(rowCount, getRowCount());
		}
	}

	void recursiveCollapseAllNodes(TreePath path, int depth) {
		TreeNode node = (TreeNode) path.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			Enumeration enumeration = node.children();
			while (enumeration.hasMoreElements()) {
				TreeNode n = (TreeNode) enumeration.nextElement();
				TreePath p = path.pathByAddingChild(n);
				recursiveCollapseAllNodes(p, depth + 1);
			}
		}
		if (depth > 0) {
			collapsePath(path);
		}
	}

	// Returns the selected prefab. Returns null if not prefab is selected.
	public ContentIndex.Prefab getSelectedPrefab() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
		if (node == null)
			return null;
		Object node_data = node.getUserObject();
		if (node_data instanceof ContentIndex.Prefab) {
			return (ContentIndex.Prefab) node_data;
		}
		return null;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {

	}

	@Override
	public void mouseExited(MouseEvent arg0) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			myPopupEvent(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			myPopupEvent(e);
	}

	private void myPopupEvent(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		TreePath path = getPathForLocation(x, y);
		if (path == null)
			return;
		setSelectionPath(path);
		Object node = path.getLastPathComponent();
		if (node == null)
			return;
		Object user_node = ((DefaultMutableTreeNode) node).getUserObject();
		if (user_node == null)
			return;
		if (user_node instanceof ContentIndex.Directory) {
			ContentIndex.Directory casted_node = (ContentIndex.Directory) user_node;
			JPopupMenu popup = new JPopupMenu(casted_node.toString());
			popup.add(new JMenuItem("Copy path to clipboard"))
					.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String string_path = casted_node.file.getAbsolutePath();
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							StringSelection content = new StringSelection(string_path);
							clipboard.setContents(content, null);
						}
					});
			popup.show(this, x, y);
		} else if (user_node instanceof ContentIndex.JsonFile) {
			ContentIndex.JsonFile casted_node = (ContentIndex.JsonFile) user_node;
			JPopupMenu popup = new JPopupMenu(casted_node.toString());
			popup.add(new JMenuItem("Copy path to clipboard"))
					.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String string_path = casted_node.file.getAbsolutePath();
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							StringSelection content = new StringSelection(string_path);
							clipboard.setContents(content, null);
						}
					});
			popup.show(this, x, y);
		} else if (user_node instanceof ContentIndex.Prefab) {
			Prefab casted_node = (ContentIndex.Prefab) user_node;
			JPopupMenu popup = new JPopupMenu(casted_node.toString());
			popup.add(new JMenuItem("Edit")).addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					parent.openEditor(casted_node);
				}
			});
			popup.add(new JMenuItem("Copy name to clipboard"))
					.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String string_path = casted_node.base_name;
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							StringSelection content = new StringSelection(string_path);
							clipboard.setContents(content, null);
						}
					});
			popup.show(this, x, y);
		} else if (user_node instanceof ContentIndex.Palette) {
			ContentIndex.Palette casted_node = (ContentIndex.Palette) user_node;
			JPopupMenu popup = new JPopupMenu(casted_node.toString());
			popup.add(new JMenuItem("Copy path to clipboard"))
					.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String string_path = casted_node.file.getAbsolutePath();
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							StringSelection content = new StringSelection(string_path);
							clipboard.setContents(content, null);
						}
					});
			popup.show(this, x, y);
		}
	}

}
