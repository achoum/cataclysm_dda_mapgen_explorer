
package cataclysm_dda_prefab_explorer.window;

import cataclysm_dda_prefab_explorer.component.ContentIndexTree;
import cataclysm_dda_prefab_explorer.component.PrefabRenderPanel;
import cataclysm_dda_prefab_explorer.content_index.ContentIndex;
import cataclysm_dda_prefab_explorer.resources_loader.Resources;
import cataclysm_dda_prefab_explorer.resources_loader.Tiles;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

// The main window. Shows the list of available content as well as a render of a selected prefab.
public class PreviewExplorer extends JFrame {

	String main_directory;
	private PrefabRenderPanel prefab_preview;
	private ContentIndexTree prefab_list;
	private JMenu mnTilesets;

	public PreviewExplorer(String main_directory) {
		setIconImage(Toolkit.getDefaultToolkit().getImage(PreviewExplorer.class.getResource("/cataclysm_dda_prefab_explorer/resources/prefab.png")));
		this.main_directory = main_directory;

		setTitle("Cataclysm dda Prefab Explorer - " + main_directory);
		setBounds(100, 100, 1024, 768);
		getContentPane().setLayout(new BorderLayout());
		{
			JSplitPane splitPane = new JSplitPane();
			splitPane.setResizeWeight(0.3);
			getContentPane().add(splitPane, BorderLayout.CENTER);
			{
				JScrollPane prefab_scroll = new JScrollPane();
				splitPane.setLeftComponent(prefab_scroll);
				{
					prefab_list = new ContentIndexTree();
					prefab_list.addTreeSelectionListener(new TreeSelectionListener() {
						@Override
						public void valueChanged(TreeSelectionEvent arg0) {
							reloadSelectedPrefab();
						}
					});
					prefab_list.setRootVisible(false);
					prefab_scroll.setViewportView(prefab_list);
				}
			}
			{
				prefab_preview = new PrefabRenderPanel();
				splitPane.setRightComponent(prefab_preview);
			}
		}
		{
			JMenuBar menuBar = new JMenuBar();
			setJMenuBar(menuBar);
			{
				JMenu mnFile = new JMenu("File");
				menuBar.add(mnFile);
				{
					JMenuItem mntmRefreshPrefabList = new JMenuItem("Reload Prefab List");
					mnFile.add(mntmRefreshPrefabList);
					{
						JMenuItem mntmNewMenuItem = new JMenuItem("Reload Selected Prefab");
						mntmNewMenuItem.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent arg0) {
								reloadSelectedPrefab();
							}
						});
						mnFile.add(mntmNewMenuItem);
					}
					{
						JSeparator separator = new JSeparator();
						mnFile.add(separator);
					}
					{
						JMenuItem mntmQuit = new JMenuItem("Quit");
						mntmQuit.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								dispose();
							}
						});
						mnFile.add(mntmQuit);
					}
					mntmRefreshPrefabList.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							refreshPrefabList();
						}
					});
				}
			}
			{
				JMenu mnView = new JMenu("View");
				menuBar.add(mnView);
				{
					mnTilesets = new JMenu("Tilsets");
					mnView.add(mnTilesets);
				}
			}
			{
				JMenu mnHelp = new JMenu("Help");
				menuBar.add(mnHelp);
				{
					JMenuItem mntmAbout = new JMenuItem("About");
					mntmAbout.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							About about = new About(PreviewExplorer.this);
							about.setLocationRelativeTo(PreviewExplorer.this);
							about.setVisible(true);
						}
					});
					mnHelp.add(mntmAbout);
				}
			}
		}

		finalizeLoading();
	}

	void finalizeLoading() {
		refreshPrefabList();
		listTilesets();
	}

	void listTilesets() {
		mnTilesets.removeAll();
		ArrayList<String> tilesets = Tiles.listTilesets(main_directory);
		for (String tileset : tilesets) {
			JMenuItem tileset_menu_item = new JMenuItem(tileset);
			mnTilesets.add(tileset_menu_item);
			tileset_menu_item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setTileset(tileset);
				}
			});
		}
	}

	public void reloadSelectedPrefab() {
		ContentIndex.Prefab newly_selected_prefab = prefab_list.getSelectedPrefab();
		if (newly_selected_prefab != null) {
			try {
				prefab_preview.loadPrefab(main_directory, newly_selected_prefab);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	void setTileset(String tileset) {
		System.out.println("Change tileset to " + tileset);
		try {
			Resources.tiles.load(main_directory, tileset);
			reloadSelectedPrefab();
			prefab_preview.refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void refreshPrefabList() {
		prefab_list.refresh(main_directory);
	}
}
