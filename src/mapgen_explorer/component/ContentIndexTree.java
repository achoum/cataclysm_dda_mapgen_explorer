
package mapgen_explorer.component;

import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.content_index.ContentIndex.Directory;
import mapgen_explorer.content_index.ContentIndex.JsonFile;
import mapgen_explorer.content_index.ContentIndex.Prefab;
import mapgen_explorer.resources_loader.Resources;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.util.Enumeration;

// Display in a JTree of the content (currently, only the mapgen json files are supported).
public class ContentIndexTree extends JTree {

	// Index of the content.
	ContentIndex content_index = new ContentIndex();

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
			}
			return default_render;
		}
	};

	public ContentIndexTree() {
		setCellRenderer(node_render);
	}

	boolean matchFilter(String filter, String value) {
		if (filter.isEmpty())
			return true;
		return value.contains(filter);
	}

	// Fill the displayed tree with the directories in the indexed content.
	boolean recursiveBuildTree(ContentIndex.Directory directory, DefaultMutableTreeNode node,
			String filter) {
		boolean match_found = matchFilter(filter, directory.file.getName());
		for (JsonFile json_file : directory.json_files) {
			DefaultMutableTreeNode json_file_node = new DefaultMutableTreeNode(json_file);
			if (recursiveBuildTree(json_file, json_file_node, filter)) {
				node.add(json_file_node);
				match_found = true;
			}
		}
		for (Directory sub_directory : directory.sub_directories) {
			DefaultMutableTreeNode sub_directory_node = new DefaultMutableTreeNode(sub_directory);
			if (recursiveBuildTree(sub_directory, sub_directory_node, filter)) {
				match_found = true;
				node.add(sub_directory_node);
			}
		}
		return match_found;
	}

	// Fill the displayed tree with the json files in the indexed content.
	boolean recursiveBuildTree(ContentIndex.JsonFile json_file, DefaultMutableTreeNode node,
			String filter) {
		boolean match_found = matchFilter(filter, json_file.file.getName());
		for (Prefab prefab : json_file.prefabs) {
			if (matchFilter(filter, prefab.toString())) {
				DefaultMutableTreeNode prefab_node = new DefaultMutableTreeNode(prefab);
				node.add(prefab_node);
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
		recursiveBuildTree(content_index.root, root, filter);
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

}
