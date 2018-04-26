
package cataclysm_dda_prefab_explorer.component;

import cataclysm_dda_prefab_explorer.content_index.ContentIndex;
import cataclysm_dda_prefab_explorer.content_index.ContentIndex.Directory;
import cataclysm_dda_prefab_explorer.content_index.ContentIndex.JsonFile;
import cataclysm_dda_prefab_explorer.content_index.ContentIndex.Prefab;
import cataclysm_dda_prefab_explorer.resources_loader.Resources;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

// Display of the available content (currently, only the prefabs).
public class ContentIndexTree extends JTree {

	// Index of the prefabs.
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

	// Fill the displayed tree with the directories in the indexed content.
	void recursiveBuildTree(ContentIndex.Directory directory, DefaultMutableTreeNode node) {
		for (JsonFile json_file : directory.json_files) {
			DefaultMutableTreeNode json_file_node = new DefaultMutableTreeNode(json_file);
			node.add(json_file_node);
			recursiveBuildTree(json_file, json_file_node);
		}
		for (Directory sub_directory : directory.sub_directories) {
			DefaultMutableTreeNode sub_directory_node = new DefaultMutableTreeNode(sub_directory);
			node.add(sub_directory_node);
			recursiveBuildTree(sub_directory, sub_directory_node);
		}
	}

	// Fill the displayed tree with the json files in the indexed content.
	void recursiveBuildTree(ContentIndex.JsonFile json_file, DefaultMutableTreeNode node) {
		for (Prefab prefab : json_file.prefabs) {
			DefaultMutableTreeNode prefab_node = new DefaultMutableTreeNode(prefab);
			node.add(prefab_node);
		}
	}

	// Re-index the content and refresh the display.
	public void refresh(String main_directory) {
		content_index.update(main_directory);
		fillFromIndex();
		expandAllNodes(0, getRowCount());
	}

	// Fill the displayed tree with the indexed content.
	void fillFromIndex() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(content_index.root);
		recursiveBuildTree(content_index.root, root);
		DefaultTreeModel model = new DefaultTreeModel(root);
		setModel(model);
	}

	void expandAllNodes(int startingIndex, int rowCount) {
		for (int i = startingIndex; i < rowCount; ++i) {
			expandRow(i);
		}
		if (getRowCount() != rowCount) {
			expandAllNodes(rowCount, getRowCount());
		}
	}

	// Returns the selected prefab. Returns null if not prefab is selected.
	public ContentIndex.Prefab getSelectedPrefab() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
		if (node == null)
			return null;
		Object node_data = node.getUserObject();
		if (node_data instanceof ContentIndex.Prefab) {
			ContentIndex.Prefab prefab = (ContentIndex.Prefab) node_data;
			return prefab;
		}
		return null;
	}

}
