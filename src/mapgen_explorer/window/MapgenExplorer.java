
package mapgen_explorer.window;

import mapgen_explorer.Config;
import mapgen_explorer.component.ContentIndexTree;
import mapgen_explorer.component.PrefabRenderPanel;
import mapgen_explorer.content_index.ContentIndex;
import mapgen_explorer.content_index.ContentIndex.JsonFile;
import mapgen_explorer.content_index.ContentIndex.Prefab;
import mapgen_explorer.resources_loader.JsonValidator;
import mapgen_explorer.resources_loader.Resources;
import mapgen_explorer.resources_loader.Tiles;
import mapgen_explorer.utils.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import net.miginfocom.swing.MigLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Paths;

// The main window. Shows the list of available content as well as a render of a selected prefab.
public class MapgenExplorer extends JFrame implements WindowListener {

	String main_directory;
	private PrefabRenderPanel prefab_preview;
	private ContentIndexTree prefab_list;
	private JMenu mnTilesets;
	private JTextField tree_filter;
	public ArrayList<Editor> editors = new ArrayList<>();

	public MapgenExplorer(String main_directory) {
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(
				MapgenExplorer.class.getResource("/mapgen_explorer/resources/prefab.png")));
		this.main_directory = main_directory;

		setTitle("Mapgen Explorer " + Config.localVersion() + " for Cataclysm DDA - "
				+ main_directory);
		setBounds(100, 100, 742, 581);
		getContentPane().setLayout(new BorderLayout());
		{
			JSplitPane splitPane = new JSplitPane();
			splitPane.setResizeWeight(0.3);
			getContentPane().add(splitPane, BorderLayout.CENTER);
			{
				prefab_preview = new PrefabRenderPanel();
				splitPane.setRightComponent(prefab_preview);
				prefab_preview.setLayout(new BorderLayout(0, 0));
				{
					JToolBar toolBar = new JToolBar();
					prefab_preview.add(toolBar, BorderLayout.NORTH);
					{
						JButton btnOpenInEditor = new JButton("Edit");
						btnOpenInEditor.setIcon(new ImageIcon(MapgenExplorer.class
								.getResource("/mapgen_explorer/resources/edit.png")));
						btnOpenInEditor.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								menuOpenInEditor();
							}
						});
						toolBar.add(btnOpenInEditor);
					}
					{
						JButton btnReload = new JButton("Reload");
						btnReload.setIcon(new ImageIcon(MapgenExplorer.class
								.getResource("/mapgen_explorer/resources/refresh.png")));
						btnReload.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								reloadSelectedPrefab();
							}
						});
						toolBar.add(btnReload);
					}
				}
			}
			{
				JPanel panel = new JPanel();
				splitPane.setLeftComponent(panel);
				panel.setLayout(new MigLayout("insets 0", "[grow]", "[][grow]"));
				{
					JScrollPane prefab_scroll = new JScrollPane();
					panel.add(prefab_scroll, "cell 0 1,grow");
					{
						prefab_list = new ContentIndexTree(this);
						prefab_scroll.setViewportView(prefab_list);
						prefab_list.addTreeSelectionListener(new TreeSelectionListener() {
							@Override
							public void valueChanged(TreeSelectionEvent arg0) {
								reloadSelectedPrefab();
							}
						});
						prefab_list.setRootVisible(false);
					}
				}
				{
					tree_filter = new JTextField() {
						@Override
						public void paintComponent(Graphics g) {
							super.paintComponent(g);
							if (getText().isEmpty()) {
								g.setColor(new Color(150, 150, 150));
								g.drawString("filter...", 10, g.getFontMetrics().getHeight());
							}
						}
					};
					panel.add(tree_filter, "flowx,cell 0 0,growx");
					tree_filter.getDocument().addDocumentListener(new DocumentListener() {
						@Override
						public void changedUpdate(DocumentEvent e) {
							prefab_list.filter(tree_filter.getText());
						}

						@Override
						public void removeUpdate(DocumentEvent e) {
							prefab_list.filter(tree_filter.getText());
						}

						@Override
						public void insertUpdate(DocumentEvent e) {
							prefab_list.filter(tree_filter.getText());
						}
					});
					tree_filter.setColumns(10);
				}
				{
					JButton btnNewButton = new JButton("");
					btnNewButton.setIcon(new ImageIcon(MapgenExplorer.class
							.getResource("/mapgen_explorer/resources/expand.png")));
					btnNewButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							prefab_list.expandAllNodes();
						}
					});
					panel.add(btnNewButton, "cell 0 0");
				}
				{
					JButton button = new JButton("");
					button.setIcon(new ImageIcon(MapgenExplorer.class
							.getResource("/mapgen_explorer/resources/collapse.png")));
					button.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							prefab_list.collapseAllNodes();
						}
					});
					panel.add(button, "cell 0 0");
				}
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
				JMenu mnAnalysis = new JMenu("Analysis");
				menuBar.add(mnAnalysis);
				{
					JMenuItem mntmScanForPotential = new JMenuItem("Scan for Potential Errors");
					mntmScanForPotential.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							scanForPotentialErrors();
						}
					});
					mnAnalysis.add(mntmScanForPotential);
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
							About about = new About(MapgenExplorer.this);
							about.setLocationRelativeTo(MapgenExplorer.this);
							about.setVisible(true);
						}
					});
					{
						JMenuItem mntmCheckForNew = new JMenuItem("Check for New Versions");
						mntmCheckForNew.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent arg0) {
								checkForNewVersion(true);
							}
						});
						mnHelp.add(mntmCheckForNew);
					}
					mnHelp.add(mntmAbout);
				}
			}
		}

		finalizeLoading();
	}

	protected void scanForPotentialErrors() {
		FileOutputStream fos;
		try {
			String summary_path = Paths.get(main_directory, "mapgen_explorer_analysis.html")
					.toString();
			File fout = new File(summary_path);
			fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(
					"This file was generated by <a href=\"https://github.com/achoum/cataclysm_dda_mapgen_explorer\">MapGen Explorer</a> v"
							+ Config.localVersion() + "."
							+ " This files contains various potential errors in the json configs."
							+ " MapGen reimplements its own parsing code. Therefore, some valid features of CDDA might not be implemented and show are erronouse."
							+ " Visit <a href=\"https://discourse.cataclysmdda.org/t/prefab-explorer/15347/14\">MapGen's forum thread</a> for more details");
			bw.write("<br/>");
			bw.write("<br/>");

			BufferedImage tmp_buffer = new BufferedImage(32 * 24, 32 * 24,
					BufferedImage.TYPE_INT_ARGB);

			prefab_list.content_index.iterateOnPrefabs(new ContentIndex.PrefabCallBack() {
				@Override
				public void call(JsonFile json) {
					try {
						String relative = new File(main_directory).toURI()
								.relativize(json.file.toURI()).getPath();
						String absolute = json.file.getAbsolutePath();
						String error_strings = JsonValidator.checkError(main_directory, json.file,
								tmp_buffer);
						if (error_strings != null && !error_strings.isEmpty()) {
							bw.write("<a href=\"" + absolute + "\">" + relative + "</a><br/><br/>");
							bw.write(error_strings);
							bw.write("<br/>");
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});

			bw.close();
			JOptionPane.showMessageDialog(this, "The summary was exported to:\"" + summary_path);
		} catch (IOException e) {
			Logger.fatal(e);
		}

	}

	protected void menuOpenInEditor() {
		ContentIndex.Prefab newly_selected_prefab = prefab_list.getSelectedPrefab();
		if (newly_selected_prefab != null) {
			openEditor(newly_selected_prefab);
		}
	}

	public void openEditor(Prefab prefab) {
		Editor frame = new Editor(main_directory);
		editors.add(frame);
		frame.setVisible(true);
		try {
			frame.open(prefab);
		} catch (Exception e) {
			Logger.fatal(e);
		}
	}

	void checkForNewVersion(boolean force) {

		Thread check_version = new Thread() {
			@Override
			public void run() {
				String local_version = Config.localVersion();
				if (local_version.equals(Config.unknown_local_version)) {
					if (force) {
						JOptionPane.showMessageDialog(MapgenExplorer.this,
								"Cannot determine the current version");
					}
					return;
				}
				String remote_version = Config.remoteVersion();
				if (remote_version == null) {
					if (force) {
						JOptionPane.showMessageDialog(MapgenExplorer.this,
								"Cannot determine the latest version");
					}
					return;
				}
				String ignore_remote_version = Config.preferences.get("ignore_remote_version",
						"NONE");
				System.out.println("Local version:" + local_version + " remote version: "
						+ remote_version + " ignore remote version:" + ignore_remote_version);
				if (!local_version.equals(remote_version)
						&& (force || !ignore_remote_version.equals(remote_version))) {
					String message = "A new version of Mapgen Explorer is available.\n"
							+ "Latest version: " + remote_version + "\n" + "Your version: "
							+ local_version;

					Object[] options = { "Download latest version", "Remind me next time",
							"Don't reming me for version " + remote_version };
					int option_idx = JOptionPane.showOptionDialog(MapgenExplorer.this, message,
							"New version available", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					System.out.println("option_idx:" + option_idx);
					if (option_idx == 0) {
						try {
							Desktop.getDesktop().browse(new URI(Config.download_webpage));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (option_idx == 1) {
						Config.preferences.put("ignore_remote_version", "NONE");
					} else if (option_idx == 2) {
						Config.preferences.put("ignore_remote_version", remote_version);
					}
				} else {
					if (force) {
						JOptionPane.showMessageDialog(MapgenExplorer.this,
								"You have the latest version.");
					}
				}
			}
		};
		check_version.run();

	}

	void finalizeLoading() {
		addWindowListener(this);
		refreshPrefabList();
		listTilesets();
		checkForNewVersion(false);
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

		JMenuItem tileset_menu_item = new JMenuItem("ASCII");
		mnTilesets.add(tileset_menu_item);
		tileset_menu_item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setTileset("ASCII");
			}
		});

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
			prefab_preview.askRedraw();
		} catch (Exception e) {
			Logger.fatal(e);
		}
	}

	void refreshPrefabList() {
		prefab_list.refresh(main_directory);
	}

	boolean hasSubEditorsOpen() {
		for (Editor editor : editors) {
			if (editor.isDisplayable()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		if (hasSubEditorsOpen()) {
			JOptionPane.showMessageDialog(this, "Close all the editor windows before.");
		} else {
			dispose();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

}
