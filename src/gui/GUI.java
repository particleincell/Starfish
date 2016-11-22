package gui;

import gui.DomainUniformModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ImageIcon;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.omg.CORBA.INITIALIZE;

import starfish.core.boundaries.Boundary;
import starfish.core.common.Starfish;
import starfish.core.domain.EllipticMesh;
import starfish.core.domain.Mesh;
import starfish.core.domain.UniformMesh;
import starfish.core.domain.DomainModule.DomainType;
import starfish.core.gui.DefaultExceptionHandler;
import starfish.core.gui.DomainEllipticModel;
import starfish.core.gui.InputTextBox;
import starfish.core.gui.InteractionsChemicalModel;
import starfish.core.gui.InteractionsDSMCModel;
import starfish.core.gui.InteractionsMCCModel;
import starfish.core.gui.InteractionsSurfaceModel;
import starfish.core.gui.MaterialKineticModel;
import starfish.core.gui.MaterialSolidModel;
import starfish.core.gui.OutputModel;
import starfish.core.interactions.ChemicalReaction;
import starfish.core.interactions.VolumeInteraction;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.Material;
import starfish.core.materials.SolidMaterial;


/**
 * The GUI is used to display Starfish, display the data parsed from the XML files and interact with the data.
 * 
 * This modification changes the regular starfish workflow to allow GUI operations.
 * Creates a new starfish and runs new starfish.initStarfish()
 * Redirects system.out to append the console on the GUI. (If you need to test and return the output to the regular console, comment out the method redirectSystemStreams)
 * 
 * If loading from file, parses file and displays data in spreadsheets on tabs
 * If you want to add data, this GUI can add data directly to Starfish by the method in the actionListeners (plus.addActionListener....)
 * 
 * @version 0.9.0
 * @author Collin Chew
 *
 */
public class GUI {
	//
	boolean testing = false;

	Starfish starfish = new Starfish();
	SimpleAttributeSet black;
	SimpleAttributeSet red;
	static JFrame frame;
	JTabbedPane tabbedPane;
	JTextPane output;
	JScrollPane scrollPane;
	static String[] userArgs;
	JTextField numField;
	JTextField dtField;

	DomainUniformModel domainUniformModel;
	DomainEllipticModel domainEllipticModel;
	MaterialKineticModel materialKineticModel;
	MaterialSolidModel materialSolidModel;
	DefaultTableModel boundaryModel;
	InteractionsChemicalModel interactionsChemicalModel;
	InteractionsMCCModel interactionsMMCModel;
	InteractionsDSMCModel interactionsDSMCModel;
	InteractionsSurfaceModel interactionsSurfaceModel;
	OutputModel outputModel;

	ArrayList<JComponent> uniformDomainSwingItems;
	ArrayList<JComponent> ellipticDomainSwingItems;
	ArrayList<JComponent> solidMaterialSwingItems;
	ArrayList<JComponent> kineticMaterialSwingItems;
	ArrayList<JComponent> chemicalInteractionsSwingItems;
	ArrayList<JComponent> mmcInteractionsSwingItems;
	ArrayList<JComponent> dsmcInteractionsSwingItems;
	ArrayList<JComponent> surfaceInteractionsSwingItems;

	JComboBox domainList;
	Properties props;
	static boolean loadedProperties = false;

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 * @param args 
	 */
	public static void createAndShowGUI(String[] args) {
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
    
		//Create and set up the window.
		frame = new JFrame("Starfish");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Create and set up the content pane.
		GUI demo = new GUI();
		demo.initializeGUIItems();

		frame.setJMenuBar(demo.createMenuBar());
		frame.setContentPane(demo.createContentPane());

		//Display the window.
		frame.setLocation(150, 50);
		frame.setSize(800, 750);
		frame.setVisible(true);

		frame.setIconImage(new ImageIcon("/icon.jpg").getImage());

		userArgs = new String[args.length];
		int i = 0;
		for(String arg: args) {
			userArgs[i] = arg;
			i++;
		}

		demo.loadDataFromDefaultFile();
		
		//Since loading data isn't working now, this will load the current working directory starfish.xml
		demo.loadDataFromFile("starfish.xml");
	}

	public void initializeGUIItems() {
		if(!testing)
			redirectSystemStreams();
		defineSetTypes();

		starfish = new Starfish();
		//starfish.initStarfish(null);

		props = new Properties();
		loadProperties();
		

	}

	/**
	 * If there are loaded properties and if the checkbox for usedefaultstarfish is true, this method will load data from the default starfish file.
	 */
	public void loadDataFromDefaultFile() {
		if(loadedProperties) {
			if(props.getProperty("usedefaultstarfish") != null) {
				if(props.getProperty("usedefaultstarfish").equals("true")) {
					loadDataFromFile(props.getProperty("defaultstarfishfile"));
				}
			}
		}
	}

	/**
	 * Loads the config.properties file which stores properties
	 */
	public void loadProperties() {
		try {
			props.load(new FileInputStream("config.properties"));
			loadedProperties = true;
		} catch (FileNotFoundException e) {
			loadedProperties = false;
			props = new Properties();
		} catch (IOException e) {
			loadedProperties = false;
			props = new Properties();
		}
	}

	/**
	 * Creates the menu bar.
	 * 
	 * TODO: Huge disclaimer: Whooops. Loading XML data from a starfish.xml doesn't really work.
	 * It can load the starfish.xml from the current working directory, but if you go outside of it, then due to the way that
	 * starfish already runs, it can't load the other files and assumes you want the files from the current working directory.
	 * Needs fixing or workaround
	 * 
	 * @return the JMenuBar
	 */
	public JMenuBar createMenuBar() {
		JMenuBar menuBar;
		JMenu menu;
		JMenuItem menuItem;
		JRadioButtonMenuItem rbMenuItem;

		//Create the menu bar.
		menuBar = new JMenuBar();

		//Build the first menu.
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menu);

		//a group of JMenuItems
		menuItem = new JMenuItem("Load xml data from starfish.xml");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
//				final JFileChooser fc = new JFileChooser(props.getProperty("filefolder"));
//				int returnVal = fc.showOpenDialog(frame);
//
//				if (returnVal == JFileChooser.APPROVE_OPTION) {
//					File file = fc.getSelectedFile();
//					starfish = new Starfish();
//					starfish.initStarfish(null);
//					loadDataFromFile(file.getAbsolutePath());
//					props.setProperty("filefolder", file.getParent());
//				}
			}			
		});

		menu.add(menuItem);

		menuItem = new JMenuItem("Clear all data");
		menuItem.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				int value = JOptionPane.showConfirmDialog(
						null, "This operation will clear all the data from this simulation.",
						"\n", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
						);

				if (value == JOptionPane.OK_OPTION) {
					starfish = new Starfish();
					//starfish.initStarfish(null);
					domainUniformModel.setRowCount(0); 
					domainEllipticModel.setRowCount(0); 
					materialKineticModel.setRowCount(0);
					materialSolidModel.setRowCount(0);
					boundaryModel.setRowCount(0);
					interactionsChemicalModel.setRowCount(0);
					interactionsMMCModel.setRowCount(0);
					interactionsDSMCModel.setRowCount(0);
					interactionsSurfaceModel.setRowCount(0);
					outputModel.setRowCount(0);
				}
			}
		});  
		menu.add(menuItem);

		menu.addSeparator();

		menuItem = new JMenuItem("Set default load starfish.xml");
		menuItem.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				//Commented out because loading doesn't work right now
//				final JFileChooser fc = new JFileChooser(props.getProperty("defaultstarfishfilefolder"));
//				int returnVal = fc.showOpenDialog(frame);
//
//				if (returnVal == JFileChooser.APPROVE_OPTION) {
//					File file = fc.getSelectedFile();
//					props.setProperty("defaultstarfishfile", file.getAbsolutePath());
//					props.setProperty("defaultstarfishfilefolder", file.getParent());
//					try {
//						props.store(new FileOutputStream("config.properties"), null);
//					} catch (FileNotFoundException e1) {} 
//					catch (IOException e1) {}

				//}
			}
		});  
		menu.add(menuItem);
		rbMenuItem = new JRadioButtonMenuItem("Use default starfish.xml (loads it now and subsequent runs)");
		if(props.getProperty("usedefaultstarfish") != null) {
			rbMenuItem.setSelected(Boolean.valueOf(props.getProperty("usedefaultstarfish")));
		}

		rbMenuItem.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{
				//Commented out because loading doesn't work right now
			}
//				JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
//				if(item.isSelected()) {
//					props.setProperty("usedefaultstarfish", "true");
//					if(props.getProperty("defaultstarfishfile") != null) {
//						loadDataFromFile(props.getProperty("defaultstarfishfile"));
//					}
//					else {
//						JOptionPane.showMessageDialog(frame,
//								"Error: Starfish has not detected a default starfish.xml file. Try setting a default file from the file menu.",
//								"No data detected",
//								JOptionPane.ERROR_MESSAGE);
//					}
//				}
//				else {
//					props.setProperty("usedefaultstarfish", "false");
//				}
//				try {
//					props.store(new FileOutputStream("config.properties"), null);
//				} catch (FileNotFoundException e1) {} 
//				catch (IOException e1) {}
//			}
		}
			);  
		menu.add(rbMenuItem);

		//Build second menu in the menu bar.
		menu = new JMenu("Edit");

		menuItem = new JMenuItem("Clear Console");
		menuItem.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				output.setText("");
			}  
		});  

		menu.add(menuItem);
		menuBar.add(menu);


		//Build second menu in the menu bar.
		menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);
		menuItem = new JMenuItem("About Starfish", KeyEvent.VK_A);
		menuItem.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				JOptionPane.showMessageDialog(frame, "Starfish created by Particle in Cell, LLC. \n http://www.particleincell.com", "About", 1);
			}  
		});  

		menu.add(menuItem);

		menuBar.add(menu);

		return menuBar;
	}

	
	/**
	 * Master GUI skeleton method.
	 * 
	 * @return
	 */
	public Container createContentPane() {


		//Create the content-pane-to-be.
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setOpaque(true);

		tabbedPane = new JTabbedPane();

		JComponent panel1 = makeDomainPanel();
		tabbedPane.addTab("Domain", null, panel1, "Define Domain");

		JComponent panel2 = makeMaterialPanel();
		tabbedPane.addTab("Materials", null, panel2, "Define Materials");

		JComponent panel3 = makeBoundariesPanel();
		tabbedPane.addTab("Boundaries", null, panel3, "Show Boundaries");

		JComponent panel4 = makeInteractionsPanel();
		tabbedPane.addTab("Interactions", null, panel4, "Define Interactions");

		JComponent panel5 = makeTimePanel();
		tabbedPane.addTab("Time", null, panel5, "Define Time Parameters");

		JComponent panel6 = makeOutputPanel();
		tabbedPane.addTab("Outputs", null, panel6, "Define Outputs");

		//Create a scrolled text area.
		output = new JTextPane();
		output.setEditable(false);
		scrollPane = new JScrollPane(output);

		JButton button = new JButton("Run simulation");
		
		JPanel upperPanel = new JPanel(new BorderLayout());
		upperPanel.add(tabbedPane, BorderLayout.CENTER);

		JPanel middlePanel = new JPanel(new BorderLayout());

		upperPanel.add(button, BorderLayout.SOUTH);

		JPanel jointPanel = new JPanel(new BorderLayout());
		jointPanel.add(upperPanel, BorderLayout.NORTH);
		jointPanel.add(middlePanel, BorderLayout.CENTER);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setLeftComponent(upperPanel);
		splitPane.setRightComponent(scrollPane);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		//splitPane.setEnabled(false);
		splitPane.setResizeWeight(0.5);
		splitPane.setDividerLocation(350);

		//Add the text area to the content pane.
		contentPane.add(splitPane, BorderLayout.CENTER);

		//This one actually calls the starfish.start and starts the main loop.
		//It will wait until the method is complete to allow the button to be pressed again
		button.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				if(true) {
					final JButton button1 = (JButton) e.getSource();
					button1.setEnabled(false);
					new Thread(new Runnable() {
						public void run() {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
								}
							});
							try{
								//output.setText("");
								button1.setEnabled(false);
								starfish.start(userArgs, null);
							}
							finally {
								button1.setEnabled(true);
							}
						}
					}).start();
				}  
				else {
					JOptionPane.showMessageDialog(frame,
							"Error: Starfish has not detected any loaded data. Try loading assets from starfish.xml from the file menu.",
							"No data detected",
							JOptionPane.ERROR_MESSAGE);
				} 
			}
		});  

		return contentPane;
	}

	/**
	 * Creates the domain panel and domain models and domain spreadsheets
	 * @return Domain Panel
	 */
	protected JComponent makeDomainPanel() {
		/**Initialize Elements **/

		//Initialize Table
		String[] columnNames = {"Name", "Origin 1", "Origin 2", "Spacing 1", "Spacing 2", "Nodes 1", "Nodes 2"};
		Object[][] data = null;
		final JTable domainUniformTable = new JTable(new DomainUniformModel(data, columnNames));
		domainUniformTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		domainUniformModel =  (DomainUniformModel) domainUniformTable.getModel();
		//if(testing) domainUniformModel.addRow(new Object[] {"uniformtest", "1", "2", "3", "4", "5", "6"});

		String[] columnNames2 = {"I", "need", "the", "specs", "for", "elliptic", "meshes"};
		Object[][] data2 = null;
		final JTable domainEllipticTable = new JTable(new DomainEllipticModel(data2, columnNames2));
		domainEllipticTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		domainEllipticModel = (DomainEllipticModel) domainEllipticTable.getModel();
		//if(testing) domainEllipticModel.addRow(new Object[] {"elliptictest", "1", "2", "3", "4", "5", "6"});

		final JScrollPane domainSpreadsheet = new JScrollPane(domainUniformTable);

		uniformDomainSwingItems = new ArrayList<JComponent>();
		ellipticDomainSwingItems = new ArrayList<JComponent>();

		JPanel panel = new JPanel(false);
		panel.setLayout(new GridLayout(4,0));

		JPanel domainSettingsPanel = new JPanel();
		/****/
		domainSettingsPanel.add(new JLabel("Domain: "));
		String[] domainTypes = { "xy", "rz" };
		domainList = new JComboBox(domainTypes);
		domainSettingsPanel.add(domainList);
		/****/
		panel.add(domainSettingsPanel);

		JPanel meshSettingsPanel = new JPanel();
		JLabel mesh = new JLabel("Add New Mesh:");
		mesh.setFont(new Font(mesh.getFont().getName(),Font.BOLD, mesh.getFont().getSize()));
		meshSettingsPanel.add(mesh);
		/****/
		meshSettingsPanel.add(new JLabel("Type: "));
		String[] meshTypes = { "", "Uniform", "Elliptic" };
		final JComboBox meshList = new JComboBox(meshTypes);
		meshSettingsPanel.add(meshList);
		/****/
		//Add Uniform Swing Items
		final InputTextBox nameInputBox = new InputTextBox("Name: ", 6);
		final InputTextBox originInputBox = new InputTextBox("Origin: ", 4, 4);
		final InputTextBox spacingInputBox = new InputTextBox("Spacing: ", 4, 4);
		final InputTextBox nodesInputBox = new InputTextBox("Nodes: ", 4, 4);
		/****/
		addJComponentToPanelAndListAndHide(nameInputBox.getJpanel(), meshSettingsPanel, uniformDomainSwingItems);
		addJComponentToPanelAndListAndHide(originInputBox.getJpanel(), meshSettingsPanel, uniformDomainSwingItems);
		addJComponentToPanelAndListAndHide(spacingInputBox.getJpanel(), meshSettingsPanel, uniformDomainSwingItems);
		addJComponentToPanelAndListAndHide(nodesInputBox.getJpanel(), meshSettingsPanel, uniformDomainSwingItems);
		/****/
		//Add Elliptic Swing Items
		JLabel boundaryLabel = new JLabel("Boundary Cond: ");
		meshSettingsPanel.add(boundaryLabel);
		String[] bcTypes = { "Dirichlet", "Neumann", "Symmetry" };
		JComboBox bcList = new JComboBox(bcTypes);
		meshSettingsPanel.add(bcList);
		String[] wallTypes = { "left", "bottom", "right", "top" };
		JComboBox wallList = new JComboBox(wallTypes);
		meshSettingsPanel.add(wallList);
		boundaryLabel.setVisible(false);
		bcList.setVisible(false);
		wallList.setVisible(false);
		uniformDomainSwingItems.add(boundaryLabel);
		uniformDomainSwingItems.add(bcList);
		uniformDomainSwingItems.add(wallList);
		/****/
		JButton plus = new JButton("Add New Mesh");
		meshSettingsPanel.add(plus);
		plus.setVisible(false);
		uniformDomainSwingItems.add(plus);
		/****/
		panel.add(meshSettingsPanel);
		/****/
		JPanel spanel = new JPanel(new BorderLayout());
		JPanel spreadsheetSwitcherPanel = new JPanel();
		final JButton uniformSwitch = new JButton("Show Uniform Meshes");
		final JButton ellipticSwitch = new JButton("Show Elliptic Meshes");
		uniformSwitch.setEnabled(false);
		spreadsheetSwitcherPanel.add(uniformSwitch);
		spreadsheetSwitcherPanel.add(ellipticSwitch);
		spanel.add(spreadsheetSwitcherPanel, BorderLayout.SOUTH);
		panel.add(spanel);
		/****/
		panel.add(domainSpreadsheet);

		uniformSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				domainSpreadsheet.getViewport().remove(0);
				domainSpreadsheet.getViewport().add(domainUniformTable);
				uniformSwitch.setEnabled(false);
				ellipticSwitch.setEnabled(true);
			}
		});

		ellipticSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				domainSpreadsheet.getViewport().remove(0);
				domainSpreadsheet.getViewport().add(domainEllipticTable);
				uniformSwitch.setEnabled(true);
				ellipticSwitch.setEnabled(false);
			}
		});

		domainList.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if(domainList.getSelectedIndex() == 0) {
					//Starfish.domain_module.setDomainType(DomainType.XY);
				}
				else if(domainList.getSelectedIndex() == 1) {
					//Starfish.domain_module.setDomain_type(DomainType.RZ);
				}
			}

		});

		//This hides the options when selecting either elliptic or uniform add functions
		meshList.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				JComboBox meshTypeBox = (JComboBox) e.getSource();
				//User selected Uniform Item
				if(meshTypeBox.getSelectedIndex() == 1) {
					for(JComponent component: ellipticDomainSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: uniformDomainSwingItems) {
						component.setVisible(true);
					}
				} //User selected Elliptic Item
				else if(meshTypeBox.getSelectedIndex() == 2) {
					for(JComponent component: uniformDomainSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: ellipticDomainSwingItems) {
						component.setVisible(true);
					}
				} //Blank selection
				else {
					for(JComponent component: uniformDomainSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: ellipticDomainSwingItems) {
						component.setVisible(false);
					}
				}
			}
		});
		
		//Adds data to spreadsheet which adds to Starfish
		plus.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				boolean duplicateMeshName = false;
				for(Mesh mesh: Starfish.domain_module.getMeshList()) {
					if( nameInputBox.getField().equals(mesh.getName())) {
						duplicateMeshName = true;
					}
				}

				if(duplicateMeshName) {
					JOptionPane.showMessageDialog(frame,
							"Error: Mesh with this name already exists. Please rename new mesh",
							"Mesh Conflict",
							JOptionPane.ERROR_MESSAGE);
				} else {
					// {"Name", "Origin 1", "Origin 2", "Spacing 1", "Spacing 2", "Nodes 1", "Nodes 2"};
					if(meshList.getSelectedIndex() == 1) {
						if(nameInputBox.getField().isEmpty() ||
								originInputBox.getField().isEmpty() ||
								originInputBox.getField2().isEmpty() ||
								spacingInputBox.getField().isEmpty() ||
								spacingInputBox.getField2().isEmpty() ||
								nodesInputBox.getField().isEmpty() ||
								nodesInputBox.getField2().isEmpty()) {
							JOptionPane.showMessageDialog(frame,
									"Error: One or more of the fields are empty. Please enter a value.",
									"Material Missing Values",
									JOptionPane.ERROR_MESSAGE);
						} else {
							domainUniformModel.addRowChangeStarfish(new Object[]{nameInputBox.getField(),
									originInputBox.getField(),
									originInputBox.getField2(),
									spacingInputBox.getField(),
									spacingInputBox.getField2(),
									nodesInputBox.getField(),
									nodesInputBox.getField2()});
						}

					} //Add elliptic mesh
					else if (meshList.getSelectedIndex() == 2){
						//domainEllipticModel.addRow.....
					}
				}
			}
		}); 

		domainUniformTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on domain table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = domainUniformTable.rowAtPoint( p );
					ListSelectionModel model = domainUniformTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							domainEllipticModel.removeRow(rowNumber);
							domainEllipticModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							domainEllipticModel.removeAllRows();
							domainEllipticModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		domainEllipticTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on domain table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = domainUniformTable.rowAtPoint( p );
					ListSelectionModel model = domainUniformTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							domainEllipticModel.removeRow(rowNumber);
							domainEllipticModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							domainEllipticModel.removeAllRows();
							domainEllipticModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		return panel;
	}

	/**
	 * 
	 * @return
	 */
	protected JComponent makeMaterialPanel() {

		String[] mColumnNames = {"Name", "molwt", "charge", "spwt", "init"};
		Object[][] mdata = null;
		final JTable materialKineticTable = new JTable(new MaterialKineticModel(mdata, mColumnNames));
		final JScrollPane materialSpreadsheet = new JScrollPane(materialKineticTable);
		materialKineticModel = (MaterialKineticModel) materialKineticTable.getModel();

		//if(testing) materialKineticModel.addRow(new Object[]{"kineticMaterialTest", "1", "2", "3", "4"});

		String[] mSolidColumnNames = {"Name", "molwt", "density"};
		Object[][] mSolidData = null;
		final JTable materialSolidTable = new JTable(new MaterialSolidModel(mSolidData, mSolidColumnNames));
		materialSolidModel = (MaterialSolidModel) materialSolidTable.getModel();

		//if(testing) materialSolidModel.addRow(new Object[]{"solidMaterialTest", "1", "2",});

		solidMaterialSwingItems = new ArrayList<JComponent>();
		kineticMaterialSwingItems = new ArrayList<JComponent>();

		JPanel panel = new JPanel(false);
		panel.setLayout(new GridLayout(4,0));

		JPanel domainSettingsPanel = new JPanel();
		/****/
		panel.add(domainSettingsPanel);
		JPanel materialSettingsPanel = new JPanel();
		JLabel mesh = new JLabel("Add New Material:");
		mesh.setFont(new Font(mesh.getFont().getName(),Font.BOLD, mesh.getFont().getSize()));
		materialSettingsPanel.add(mesh);
		/****/
		materialSettingsPanel.add(new JLabel("Type: "));
		String[] materialTypes = { "", "Kinetic", "Solid" };
		final JComboBox materialList = new JComboBox(materialTypes);
		materialSettingsPanel.add(materialList);
		/****/
		final InputTextBox nameTextBox = new InputTextBox("Name: ", 6);
		final InputTextBox moleWeightTextBox = new InputTextBox("MoleWeight: ", 3);
		final InputTextBox chargeTextBox = new InputTextBox("Charge: ", 3);
		final InputTextBox specWeightTextBox = new InputTextBox("Spec. Weight: ", 3);
		final InputTextBox initTextBox = new InputTextBox("Init: ", 3);
		final InputTextBox densityTextBox = new InputTextBox("Density: ", 3);
		/****/
		addJComponentToPanelAndListAndHide(nameTextBox.getJpanel(), materialSettingsPanel, kineticMaterialSwingItems);
		addJComponentToPanelAndListAndHide(moleWeightTextBox.getJpanel(), materialSettingsPanel, kineticMaterialSwingItems);
		addJComponentToPanelAndListAndHide(chargeTextBox.getJpanel(), materialSettingsPanel, kineticMaterialSwingItems);
		addJComponentToPanelAndListAndHide(specWeightTextBox.getJpanel(), materialSettingsPanel, kineticMaterialSwingItems);
		addJComponentToPanelAndListAndHide(initTextBox.getJpanel(), materialSettingsPanel, kineticMaterialSwingItems);
		/****/
		solidMaterialSwingItems.add(nameTextBox.getJpanel()); //Don't need to add to panel again or hide for either of these, so we just add it to the list
		solidMaterialSwingItems.add(moleWeightTextBox.getJpanel());
		addJComponentToPanelAndListAndHide(densityTextBox.getJpanel(), materialSettingsPanel, solidMaterialSwingItems);
		/****/
		JButton plus = new JButton("Add Material");
		plus.setVisible(false);
		materialSettingsPanel.add(plus);
		solidMaterialSwingItems.add(plus);
		kineticMaterialSwingItems.add(plus);


		panel.add(materialSettingsPanel);

		/****/
		JPanel spanel = new JPanel(new BorderLayout());
		JPanel spreadsheetSwitcherPanel = new JPanel();
		final JButton kineticSwitch = new JButton("Show Kinetic Materials");
		final JButton solidSwitch = new JButton("Show Solid Materials");
		kineticSwitch.setEnabled(false);
		spreadsheetSwitcherPanel.add(kineticSwitch);
		spreadsheetSwitcherPanel.add(solidSwitch);
		spanel.add(spreadsheetSwitcherPanel, BorderLayout.SOUTH);
		panel.add(spanel);
		/****/
		panel.add(materialSpreadsheet);

		kineticSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				materialSpreadsheet.getViewport().remove(0);
				materialSpreadsheet.getViewport().add(materialKineticTable);
				kineticSwitch.setEnabled(false);
				solidSwitch.setEnabled(true);
			}
		});

		solidSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				materialSpreadsheet.getViewport().remove(0);
				materialSpreadsheet.getViewport().add(materialSolidTable);
				kineticSwitch.setEnabled(true);
				solidSwitch.setEnabled(false);
			}
		});


		materialList.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				JComboBox meshTypeBox = (JComboBox) e.getSource();
				//User selected Kinetic Item
				if(meshTypeBox.getSelectedIndex() == 1) {
					for(JComponent component: solidMaterialSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: kineticMaterialSwingItems) {
						component.setVisible(true);
					}
				} //User selected Solid Item
				else if(meshTypeBox.getSelectedIndex() == 2) {
					for(JComponent component: kineticMaterialSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: solidMaterialSwingItems) {
						component.setVisible(true);
					}
				} //Blank selection
				else {
					for(JComponent component: kineticMaterialSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: solidMaterialSwingItems) {
						component.setVisible(false);
					}
				}
			}
		});

		plus.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				boolean duplicateMeshName = false;
				for(Material material: Starfish.materials_module.getMaterialsList()) {
					if( nameTextBox.getField().equals(material.getName())) {
						duplicateMeshName = true;
					}
				}

				if(duplicateMeshName) {
					JOptionPane.showMessageDialog(frame,
							"Error: Material with this name already exists. Please rename new material",
							"Material Name Conflict",
							JOptionPane.ERROR_MESSAGE);
				} 
				else {
					// Add kinetic material
					if(materialList.getSelectedIndex() == 1) {
						if (nameTextBox.getField().isEmpty() ||
								moleWeightTextBox.getField().isEmpty() ||
								chargeTextBox.getField().isEmpty() ||
								specWeightTextBox.getField().isEmpty() ||
								initTextBox.getField().isEmpty()) {
							JOptionPane.showMessageDialog(frame,
									"Error: One or more of the fields are empty. Please enter a value.",
									"Material Missing Values",
									JOptionPane.ERROR_MESSAGE);
						} else {
							materialKineticModel.addRowChangeStarfish(new Object[]{nameTextBox.getField(),
									moleWeightTextBox.getField(),
									chargeTextBox.getField(),
									specWeightTextBox.getField(),
									initTextBox.getField()});
						}
					} //Add solid material
					else if (materialList.getSelectedIndex() == 2){
						if (nameTextBox.getField().isEmpty() ||
								moleWeightTextBox.getField().isEmpty() ||
								densityTextBox.getField().isEmpty()) {
							JOptionPane.showMessageDialog(frame,
									"Error: One or more of the fields are empty. Please enter a value.",
									"Material Missing Values",
									JOptionPane.ERROR_MESSAGE);
						} else {
							materialSolidModel.addRowChangeStarfish(new Object[]{nameTextBox.getField(),
									moleWeightTextBox.getField(),
									densityTextBox.getField()});
						}
					}
				}
			}
		}); 

		materialKineticTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on domain table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = materialKineticTable.rowAtPoint( p );
					ListSelectionModel model = materialKineticTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );

					JPopupMenu menu = new JPopupMenu();
					//					JMenuItem viewUniformItems = new JMenuItem("View Kinetic Material Items");
					//					viewUniformItems.addActionListener(new ActionListener() {
					//						@Override
					//						public void actionPerformed(ActionEvent arg0) {
					//							materialSpreadsheet.getViewport().remove(0);
					//							materialSpreadsheet.getViewport().add(materialKineticTable);
					//						}
					//					});
					//					menu.add(viewUniformItems);
					//					JMenuItem viewEllipticItems = new JMenuItem("View Solid Material Items");
					//					viewEllipticItems.addActionListener(new ActionListener() {
					//						@Override
					//						public void actionPerformed(ActionEvent arg0) {
					//							materialSpreadsheet.getViewport().remove(0);
					//							materialSpreadsheet.getViewport().add(materialSolidTable);
					//						}
					//					});
					//					menu.add(viewEllipticItems);
					//					menu.addSeparator();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							materialKineticModel.removeRow(rowNumber);
							materialKineticModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							materialKineticModel.removeAllRows();
							materialKineticModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		materialSolidTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on domain table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = materialKineticTable.rowAtPoint( p );
					ListSelectionModel model = materialKineticTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					//					JMenuItem viewUniformItems = new JMenuItem("View Kinetic Material Items");
					//					viewUniformItems.addActionListener(new ActionListener() {
					//						@Override
					//						public void actionPerformed(ActionEvent arg0) {
					//							materialSpreadsheet.getViewport().remove(0);
					//							materialSpreadsheet.getViewport().add(materialKineticTable);
					//						}
					//					});
					//					menu.add(viewUniformItems);
					//					JMenuItem viewEllipticItems = new JMenuItem("View Solid Material Items");
					//					viewEllipticItems.addActionListener(new ActionListener() {
					//						@Override
					//						public void actionPerformed(ActionEvent arg0) {
					//							materialSpreadsheet.getViewport().remove(0);
					//							materialSpreadsheet.getViewport().add(materialSolidTable);
					//						}
					//					});
					//					menu.add(viewEllipticItems);
					//					menu.addSeparator();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							materialSolidModel.removeRow(rowNumber);
							materialSolidModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							materialSolidModel.removeAllRows();
							materialSolidModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});


		return panel;
	}

	protected JComponent makeBoundariesPanel() {
		/**Initialize Elements **/

		//Initialize Table
		String[] columnNames = {"Name", "Type", "Value"};
		Object[][] data = null;
		final JTable boundaryTable = new JTable(new DefaultTableModel(data, columnNames));
		boundaryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		boundaryModel =  (DefaultTableModel) boundaryTable.getModel();
		//if(testing) boundaryModel.addRow(new Object[] {"boundaryTest", "1", "2"});

		final JScrollPane boundarySpreadsheet = new JScrollPane(boundaryTable);

		//JPanel panel = new JPanel();
		//panel.setLayout(new GridLayout(3,0));
		//JPanel domainSettingsPanel = new JPanel();
		/****/
		//panel.add(domainSettingsPanel);

		//JPanel meshSettingsPanel = new JPanel();
		/****/
		//JButton importButton = new JButton("Import new xml boundary file");
		//meshSettingsPanel.add(importButton);
		/****/

		/****/
		//panel.add(meshSettingsPanel);
		/****/
		//panel.add(boundarySpreadsheet);

		return boundarySpreadsheet;
	}

	protected JComponent makeTimePanel() {
		/**Initialize Elements **/

		JPanel panel = new JPanel(false);
		panel.setLayout(new GridLayout(3,0));

		JPanel domainSettingsPanel = new JPanel();
		/****/
		panel.add(domainSettingsPanel);

		JPanel meshSettingsPanel = new JPanel();
		/****/
		JLabel numLabel = new JLabel("Num_It: ");
		numLabel.setFont(new Font(numLabel.getFont().getName(),Font.BOLD, numLabel.getFont().getSize()));
		numField = new JTextField(10);

		JLabel dtLabel = new JLabel("dt: ");
		dtLabel.setFont(new Font(dtLabel.getFont().getName(),Font.BOLD, dtLabel.getFont().getSize()));
		dtField = new JTextField(10);

		meshSettingsPanel.add(numLabel);
		meshSettingsPanel.add(numField);
		meshSettingsPanel.add(dtLabel);
		meshSettingsPanel.add(dtField);
		/****/
		JPanel spanel = new JPanel();
		JButton button = new JButton("Update Values");
		spanel.add(button);
		/****/
		panel.add(meshSettingsPanel);
		panel.add(spanel);
		/****/
		button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(numField.getText().isEmpty() || dtField.getText().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Error: One or more of the fields are empty. Please enter a value.",
							"Material Missing Values",
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					Starfish.time_module.setNumIt(Integer.valueOf(numField.getText()));
					Starfish.time_module.setDt(Double.valueOf(dtField.getText()));
				}
			}
		});

		return panel;
	}

	protected JComponent makeInteractionsPanel() {

		String[] mColumnNames = {"Process", "Sources", "Products", "Rate", "Rate Coeffs"};
		Object[][] mdata = null;
		final JTable interactionsChemicalTable = new JTable(new InteractionsChemicalModel(mdata, mColumnNames));
		final JScrollPane interactionsSpreadsheet = new JScrollPane(interactionsChemicalTable);
		interactionsChemicalModel = (InteractionsChemicalModel) interactionsChemicalTable.getModel();

		//if(testing) interactionsChemicalModel.addRow(new Object[]{"interactions chemical test", "Xe", "Xe", "Xe", "Xe"});

		String[] mccColumnNames = {"Process", "Source", "Target", "Sigma", "Sigma Coeffs"};
		Object[][] mmcData = null;
		final JTable interactionsMMCTable = new JTable(new InteractionsMCCModel(mmcData, mccColumnNames));
		interactionsMMCModel = (InteractionsMCCModel) interactionsMMCTable.getModel();

		//if(testing) interactionsMMCModel.addRow(new Object[]{"interactions mmc test", "Xe", "Xe", "const", "1"});

		String[] dsmcColumnNames = {"Model", "Source", "Target", "Sigma", "Sigma Coeffs", "Sigma Max"};
		Object[][] dsmcData = null;
		final JTable interactionsDSMCTable = new JTable(new InteractionsDSMCModel(dsmcData, dsmcColumnNames));
		interactionsDSMCModel = (InteractionsDSMCModel) interactionsDSMCTable.getModel();

		//if(testing) interactionsDSMCModel.addRow(new Object[]{"interactions dsmc test", "O+", "O+", "inv", "1", "1"});

		String[] surfaceColumnNames = {"Source", "Target", "Product", "Model", "Prob", "C Accom", "C Rest"};
		Object[][] surfaceData = null;
		final JTable interactionsSurfaceTable = new JTable(new InteractionsSurfaceModel(surfaceData, surfaceColumnNames));
		interactionsSurfaceModel = (InteractionsSurfaceModel) interactionsSurfaceTable.getModel();

		//if(testing) interactionsSurfaceModel.addRow(new Object[]{"interactions surface test", "SS", "O", "specular", "1", "1", "1"});

		chemicalInteractionsSwingItems = new ArrayList<JComponent>();
		mmcInteractionsSwingItems = new ArrayList<JComponent>();
		dsmcInteractionsSwingItems = new ArrayList<JComponent>();
		surfaceInteractionsSwingItems = new ArrayList<JComponent>();

		JPanel panel = new JPanel(false);
		panel.setLayout(new GridLayout(4,0));

		JPanel domainSettingsPanel = new JPanel();
		/****/
		panel.add(domainSettingsPanel);
		JPanel materialSettingsPanel = new JPanel();
		JLabel mesh = new JLabel("Add New Interaction:");
		mesh.setFont(new Font(mesh.getFont().getName(),Font.BOLD, mesh.getFont().getSize()));
		materialSettingsPanel.add(mesh);
		/****/
		materialSettingsPanel.add(new JLabel("Type: "));
		String[] materialTypes = { "", "Chemical", "DSMC", "MMC", "Surface" };
		final JComboBox materialList = new JComboBox(materialTypes);
		materialSettingsPanel.add(materialList);
		/****/
		final InputTextBox processTextBox = new InputTextBox("Process: ", 6);
		final InputTextBox sourcesTextBox = new InputTextBox("Sources: ", 5);
		final InputTextBox sourceTextBox = new InputTextBox("Source: ", 5);
		final InputTextBox productsTextBox = new InputTextBox("Products: ", 5);
		final InputTextBox productTextBox = new InputTextBox("Product: ", 5);
		final InputTextBox rateTextBox = new InputTextBox("Rate: ", 5);
		final InputTextBox rateCoeffsTextBox = new InputTextBox("Rate Coeffs: ", 5);
		final InputTextBox targetTextBox = new InputTextBox("Target: ", 5);
		final InputTextBox sigmaTextBox = new InputTextBox("Sigma: ", 5);
		final InputTextBox probTextBox = new InputTextBox("Prob: ", 5);
		final InputTextBox modelTextBox = new InputTextBox("Model: ", 5);
		final InputTextBox emissionTextBox = new InputTextBox("Emission: ", 5);
		final InputTextBox sigmaCoeffsTextBox = new InputTextBox("Sigma Coeffs: ", 5);
		final InputTextBox sigmaMaxTextBox = new InputTextBox("Sigma Max: ", 5);
		final InputTextBox cStickTextBox = new InputTextBox("C Stick: ", 5);
		final InputTextBox cRestTextBox = new InputTextBox("C Rest: ", 5);
		final InputTextBox cAccomTextBox = new InputTextBox("C Accom: ", 5);
		final InputTextBox sputterTypeTextBox = new InputTextBox("Sputter Type: ", 5);
		final InputTextBox sputterYieldTextBox = new InputTextBox("Sputter Yield: ", 5);
		final InputTextBox sputterProductTextBox = new InputTextBox("Sputter Product: ", 5);
		/****/
		addJComponentToPanelAndListAndHide(processTextBox.getJpanel(), materialSettingsPanel, chemicalInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(sourcesTextBox.getJpanel(), materialSettingsPanel, chemicalInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(productsTextBox.getJpanel(), materialSettingsPanel, chemicalInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(rateTextBox.getJpanel(), materialSettingsPanel, chemicalInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(rateCoeffsTextBox.getJpanel(), materialSettingsPanel, chemicalInteractionsSwingItems);
		/****/
		mmcInteractionsSwingItems.add(processTextBox.getJpanel());
		addJComponentToPanelAndListAndHide(sourceTextBox.getJpanel(), materialSettingsPanel, mmcInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(targetTextBox.getJpanel(), materialSettingsPanel, mmcInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(sigmaTextBox.getJpanel(), materialSettingsPanel, mmcInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(sigmaCoeffsTextBox.getJpanel(), materialSettingsPanel, mmcInteractionsSwingItems);
		/****/
		addJComponentToPanelAndListAndHide(modelTextBox.getJpanel(), materialSettingsPanel, dsmcInteractionsSwingItems);
		dsmcInteractionsSwingItems.add(sourceTextBox.getJpanel());
		dsmcInteractionsSwingItems.add(targetTextBox.getJpanel());
		dsmcInteractionsSwingItems.add(sigmaTextBox.getJpanel());
		dsmcInteractionsSwingItems.add(sigmaCoeffsTextBox.getJpanel());
		addJComponentToPanelAndListAndHide(sigmaMaxTextBox.getJpanel(), materialSettingsPanel, dsmcInteractionsSwingItems);
		/****/
		surfaceInteractionsSwingItems.add(sourceTextBox.getJpanel());
		surfaceInteractionsSwingItems.add(targetTextBox.getJpanel());
		addJComponentToPanelAndListAndHide(emissionTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(productTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(cStickTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(cRestTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(cAccomTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(sputterTypeTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(sputterYieldTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		addJComponentToPanelAndListAndHide(sputterProductTextBox.getJpanel(), materialSettingsPanel, surfaceInteractionsSwingItems);
		/****/
		JButton plus = new JButton("Add Interaction");
		plus.setVisible(false);
		materialSettingsPanel.add(plus);
		chemicalInteractionsSwingItems.add(plus);
		mmcInteractionsSwingItems.add(plus);
		dsmcInteractionsSwingItems.add(plus);
		surfaceInteractionsSwingItems.add(plus);

		panel.add(materialSettingsPanel);

		/****/
		JPanel spanel = new JPanel(new BorderLayout());
		JPanel spreadsheetSwitcherPanel = new JPanel();
		final JButton chemicalSwitch = new JButton("Show Chemical Interactions");
		final JButton mmcSwitch = new JButton("Show MMC Interactions");
		final JButton dsmcSwitch = new JButton("Show DSMC Interactions");
		final JButton surfaceSwitch = new JButton("Show Solid Interactions");
		chemicalSwitch.setEnabled(false);
		spreadsheetSwitcherPanel.add(chemicalSwitch);
		spreadsheetSwitcherPanel.add(mmcSwitch);
		spreadsheetSwitcherPanel.add(dsmcSwitch);
		spreadsheetSwitcherPanel.add(surfaceSwitch);
		spanel.add(spreadsheetSwitcherPanel, BorderLayout.SOUTH);
		panel.add(spanel);
		/****/
		panel.add(interactionsSpreadsheet);

		chemicalSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				interactionsSpreadsheet.getViewport().remove(0);
				interactionsSpreadsheet.getViewport().add(interactionsChemicalTable);
				chemicalSwitch.setEnabled(false);
				mmcSwitch.setEnabled(true);
				dsmcSwitch.setEnabled(true);
				surfaceSwitch.setEnabled(true);
			}
		});

		mmcSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				interactionsSpreadsheet.getViewport().remove(0);
				interactionsSpreadsheet.getViewport().add(interactionsMMCTable);
				chemicalSwitch.setEnabled(true);
				mmcSwitch.setEnabled(false);
				dsmcSwitch.setEnabled(true);
				surfaceSwitch.setEnabled(true);
			}
		});

		dsmcSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				interactionsSpreadsheet.getViewport().remove(0);
				interactionsSpreadsheet.getViewport().add(interactionsDSMCTable);
				chemicalSwitch.setEnabled(true);
				mmcSwitch.setEnabled(true);
				dsmcSwitch.setEnabled(false);
				surfaceSwitch.setEnabled(true);
			}
		});

		surfaceSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				interactionsSpreadsheet.getViewport().remove(0);
				interactionsSpreadsheet.getViewport().add(interactionsSurfaceTable);
				chemicalSwitch.setEnabled(true);
				mmcSwitch.setEnabled(true);
				dsmcSwitch.setEnabled(true);
				surfaceSwitch.setEnabled(false);
			}
		});



		materialList.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				JComboBox meshTypeBox = (JComboBox) e.getSource();
				//User selected chemical interaction
				if(meshTypeBox.getSelectedIndex() == 1) {
					for(JComponent component: mmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: dsmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: surfaceInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: chemicalInteractionsSwingItems) {
						component.setVisible(true);
					}
				} //User selected mmc interaction
				else if(meshTypeBox.getSelectedIndex() == 2) {
					for(JComponent component: dsmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: surfaceInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: chemicalInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: mmcInteractionsSwingItems) {
						component.setVisible(true);
					}
				}
				//User selected dsmc interaction
				else if(meshTypeBox.getSelectedIndex() == 3) {
					for(JComponent component: surfaceInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: chemicalInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: mmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: dsmcInteractionsSwingItems) {
						component.setVisible(true);
					}
				}
				//User selected surface interaction
				else if(meshTypeBox.getSelectedIndex() == 4) {
					for(JComponent component: chemicalInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: mmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: dsmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: surfaceInteractionsSwingItems) {
						component.setVisible(true);
					}
				}//Blank selection
				else {
					for(JComponent component: chemicalInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: mmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: dsmcInteractionsSwingItems) {
						component.setVisible(false);
					}
					for(JComponent component: surfaceInteractionsSwingItems) {
						component.setVisible(false);
					}
				}
			}
		});

		plus.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  
				// Add chemical interaction
				if(materialList.getSelectedIndex() == 1) {
					if (processTextBox.getField().isEmpty() ||
							sourcesTextBox.getField().isEmpty() ||
							productsTextBox.getField().isEmpty() ||
							rateTextBox.getField().isEmpty() ||
							rateCoeffsTextBox.getField().isEmpty()) {
						JOptionPane.showMessageDialog(frame,
								"Error: One or more of the fields are empty. Please enter a value.",
								"Interaction Missing Values",
								JOptionPane.ERROR_MESSAGE);
					} else {
						interactionsChemicalModel.addRowChangeStarfish(new Object[]{processTextBox.getField(),
								sourcesTextBox.getField(),
								productsTextBox.getField(),
								rateTextBox.getField(),
								rateCoeffsTextBox.getField()});
					}
				} //Add mmc interaction
				else if (materialList.getSelectedIndex() == 2){
					if (processTextBox.getField().isEmpty() ||
							sourceTextBox.getField().isEmpty() ||
							targetTextBox.getField().isEmpty() ||
							sigmaTextBox.getField().isEmpty() ||
							sigmaCoeffsTextBox.getField().isEmpty()) {
						JOptionPane.showMessageDialog(frame,
								"Error: One or more of the fields are empty. Please enter a value.",
								"Interaction Missing Values",
								JOptionPane.ERROR_MESSAGE);
					} else {
						interactionsMMCModel.addRowChangeStarfish(new Object[]{processTextBox.getField(),
								sourceTextBox.getField(),
								targetTextBox.getField(),
								sigmaTextBox.getField(),
								sigmaCoeffsTextBox.getField()});
					}
				} //Add dsmc interaction
				else if (materialList.getSelectedIndex() == 3){
					if (modelTextBox.getField().isEmpty() ||
							sourceTextBox.getField().isEmpty() ||
							targetTextBox.getField().isEmpty() ||
							sigmaTextBox.getField().isEmpty() ||
							sigmaCoeffsTextBox.getField().isEmpty() ||
							sigmaMaxTextBox.getField().isEmpty()) {
						JOptionPane.showMessageDialog(frame,
								"Error: One or more of the fields are empty. Please enter a value.",
								"Interaction Missing Values",
								JOptionPane.ERROR_MESSAGE);
					} else {
						interactionsDSMCModel.addRowChangeStarfish(new Object[]{modelTextBox.getField(),
								sourceTextBox.getField(),
								targetTextBox.getField(),
								sigmaTextBox.getField(),
								sigmaCoeffsTextBox.getField(),
								sigmaMaxTextBox.getField(),});
					}
				}
				//Add surface interaction
				else if (materialList.getSelectedIndex() == 4){
					if (sourceTextBox.getField().isEmpty() ||
							targetTextBox.getField().isEmpty() ||
							emissionTextBox.getField().isEmpty() ||
							productTextBox.getField().isEmpty() ||
							cStickTextBox.getField().isEmpty() ||
							cRestTextBox.getField().isEmpty() ||
							cAccomTextBox.getField().isEmpty() ||
							sputterTypeTextBox.getField().isEmpty() ||
							sputterYieldTextBox.getField().isEmpty() ||
							sputterProductTextBox.getField().isEmpty()) {
						JOptionPane.showMessageDialog(frame,
								"Error: One or more of the fields are empty. Please enter a value.",
								"Interaction Missing Values",
								JOptionPane.ERROR_MESSAGE);
					} else {
						interactionsSurfaceModel.addRowChangeStarfish(new Object[]{sourceTextBox.getField(),
								targetTextBox.getField(),
								emissionTextBox.getField(),
								productTextBox.getField(),
								cStickTextBox.getField(),
								cRestTextBox.getField(),
								cAccomTextBox.getField(),
								sputterTypeTextBox.getField(),
								sputterYieldTextBox.getField(),
								sputterProductTextBox.getField()});
					}
				}

			}
		}); 

		interactionsChemicalTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on chemical interaction table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = interactionsChemicalTable.rowAtPoint( p );
					ListSelectionModel model = interactionsChemicalTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							interactionsChemicalModel.removeRow(rowNumber);
							interactionsChemicalModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							interactionsChemicalModel.removeAllRows();
							interactionsChemicalModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		interactionsMMCTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on mmc interaction table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = interactionsMMCTable.rowAtPoint( p );
					ListSelectionModel model = interactionsMMCTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							interactionsMMCModel.removeRow(rowNumber);
							interactionsMMCModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							interactionsMMCModel.removeAllRows();
							interactionsMMCModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		interactionsDSMCTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on dsmc interaction table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = interactionsDSMCTable.rowAtPoint( p );
					ListSelectionModel model = interactionsDSMCTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							interactionsDSMCModel.removeRow(rowNumber);
							interactionsDSMCModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							interactionsDSMCModel.removeAllRows();
							interactionsDSMCModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		interactionsSurfaceTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getButton()==3) {
					if(testing) {
						System.out.println("Action Detected: Right Click on surface interaction table");
					}

					//Highlight Row on rightclick
					Point p = arg0.getPoint();
					final int rowNumber = interactionsSurfaceTable.rowAtPoint( p );
					ListSelectionModel model = interactionsSurfaceTable.getSelectionModel();
					model.setSelectionInterval( rowNumber, rowNumber );


					JPopupMenu menu = new JPopupMenu();
					JMenuItem deleteRowItem = new JMenuItem("Delete Row");
					menu.add(deleteRowItem);
					deleteRowItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							if(testing) {
								System.out.println("Action Performed: Delete Row Number " + rowNumber);
							}

							interactionsSurfaceModel.removeRow(rowNumber);
							interactionsSurfaceModel.fireTableDataChanged();
						}
					});

					JMenuItem clearDataItem = new JMenuItem("Clear Data");
					menu.add(clearDataItem);
					clearDataItem.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent arg0) {
							System.out.println("Action Performed: Clear Data ");
							interactionsSurfaceModel.removeAllRows();
							interactionsSurfaceModel.fireTableDataChanged();
						}
					});

					menu.show(arg0.getComponent(), arg0.getX(), arg0.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mousePressed(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {}

		});

		return panel;
	}

	protected JComponent makeOutputPanel() {
		/**Initialize Elements **/
		//Initialize Table
		String[] columnNames = {"Type", "File Name", "Format", "Mesh", "Index", "Variables"};
		Object[][] data = null;
		final JTable outputTable = new JTable(new OutputModel(data, columnNames));
		outputTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		outputModel =  (OutputModel) outputTable.getModel();

		final JScrollPane outputSpreadsheet = new JScrollPane(outputTable);
		
		JPanel panel = new JPanel(false);
		panel.setLayout(new GridLayout(3,0));

		JPanel topPanel = new JPanel();
		JLabel animationLabel = new JLabel("Animation Output: ");
		animationLabel.setFont(new Font(animationLabel.getFont().getName(),Font.BOLD, animationLabel.getFont().getSize()));
		final InputTextBox typeInputBox = new InputTextBox("Type: ", 6);
		final InputTextBox fNameInputBox = new InputTextBox("File Name: ", 12);
		final InputTextBox formatInputBox = new InputTextBox("Format: ", 6);
		final InputTextBox variablesInputBox = new InputTextBox("Variables: ", 20);
		/****/
		topPanel.add(animationLabel);
		topPanel.add(typeInputBox.getJpanel());
		topPanel.add(fNameInputBox.getJpanel());
		topPanel.add(formatInputBox.getJpanel());
		topPanel.add(variablesInputBox.getJpanel());
		panel.add(topPanel);
        /****/
		JPanel meshSettingsPanel = new JPanel();
		/****/
		JLabel outputLabel = new JLabel("Add new Output: ");
		outputLabel.setFont(new Font(outputLabel.getFont().getName(),Font.BOLD, outputLabel.getFont().getSize()));
		final InputTextBox type2InputBox = new InputTextBox("Type: ", 6);
		final InputTextBox fName2InputBox = new InputTextBox("File Name: ", 12);
		final InputTextBox format2InputBox = new InputTextBox("Format: ", 6);
		final InputTextBox meshInputBox = new InputTextBox("Mesh: ", 6);
		final InputTextBox indexInputBox = new InputTextBox("Index: ", 6);
		final InputTextBox variables2InputBox = new InputTextBox("Variables: ", 20);
		meshSettingsPanel.add(outputLabel);
		meshSettingsPanel.add(type2InputBox.getJpanel());
		meshSettingsPanel.add(fName2InputBox.getJpanel());
		meshSettingsPanel.add(format2InputBox.getJpanel());
		meshSettingsPanel.add(meshInputBox.getJpanel());
		meshSettingsPanel.add(indexInputBox.getJpanel());
		meshSettingsPanel.add(variables2InputBox.getJpanel());
		JButton plus = new JButton("Add New Output");
		meshSettingsPanel.add(plus);
		/****/
		
		/****/
		JButton button = new JButton("Update Animation Output");
		topPanel.add(button);
		/****/
		panel.add(meshSettingsPanel);
		
		/****/
		button.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  

				if(typeInputBox.getField().isEmpty() ||
						fNameInputBox.getField().isEmpty() ||
						formatInputBox.getField().isEmpty() ||
						variablesInputBox.getField().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Error: One or more of the fields are empty. Please enter a value.",
							"Output Missing Values",
							JOptionPane.ERROR_MESSAGE);
				} else {
					//TODO:
				}

			}
		}); 
		
		
		
		plus.addActionListener(new ActionListener()  
		{  
			public void actionPerformed(ActionEvent e)  
			{  

				if(type2InputBox.getField().isEmpty() ||
						fName2InputBox.getField().isEmpty() ||
						format2InputBox.getField().isEmpty() ||
						meshInputBox.getField().isEmpty() ||
						indexInputBox.getField().isEmpty() ||
						variables2InputBox.getField().isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							"Error: One or more of the fields are empty. Please enter a value.",
							"Output Missing Values",
							JOptionPane.ERROR_MESSAGE);
				} else {
					outputModel.addRowChangeStarfish(new Object[]{type2InputBox.getField(),
							fName2InputBox.getField(),
							format2InputBox.getField(),
							meshInputBox.getField(),
							indexInputBox.getField(),
							variables2InputBox.getField()});
				}

			}
		}); 
		
		panel.add(outputSpreadsheet);

		return panel;
	}

	/**
	 * Pretty self explanatory, adds a jcomponent to a specified panel and list, then hides the jcomponent
	 * @param component
	 * @param panel
	 * @param list
	 */
	private void addJComponentToPanelAndListAndHide(JComponent component, JPanel panel, ArrayList<JComponent> list) {
		panel.add(component);
		component.setVisible(false);
		list.add(component);
	}

	/**
	 * This method processes the input file specified if it has a filepath, and then populates the spreadsheets with glorious data
	 * @param filepath
	 */
	private void loadDataFromFile(String filepath) {
		if(filepath != null) {
			starfish = new Starfish();
			//starfish.ProcessInputFile(filepath);
			try {
				props.store(new FileOutputStream("config.properties"), null);
			} catch (FileNotFoundException e) {} 
			catch (IOException e) {}

			/*******/
			/*******/
			/** Load Domain Information and put it in domain model **/
			/*******/
			/*******/

			if(Starfish.domain_module.getDomainType().toString().equals("XY")) {
				domainList.setSelectedIndex(0); } 
			else if (Starfish.domain_module.getDomainType().toString().equals("RX")){ 
				domainList.setSelectedIndex(1); }

			ArrayList<Mesh> meshes = Starfish.domain_module.getMeshList();
			int size = meshes.size();
			for(int i = 0; i < size; i++) {
				//Load uniform meshes
				if(meshes.get(i).getClass().getName().contains("UniformMesh")) {
					UniformMesh mesh = (UniformMesh) meshes.get(i);
					domainUniformModel.addRow(new Object[]{mesh.getName(), 
							String.valueOf(mesh.x0[0]), 
							String.valueOf(mesh.x0[1]), 
							String.valueOf(mesh.dh[0]), 
							String.valueOf(mesh.dh[1]), 
							String.valueOf(mesh.ni), 
							String.valueOf(mesh.nj)});
					domainUniformModel.fireTableDataChanged();
				}
				//Load elliptic meshes
				else {
					EllipticMesh mesh = (EllipticMesh) meshes.get(i);
				}
			}

			/*******/
			/*******/
			/** Load Material Information and put it in material models **/
			/*******/
			/*******/

			ArrayList<Material> materials = Starfish.materials_module.getMaterialsList();
			int msize = materials.size();
			for(int i = 0; i < msize; i++) {
				//Load kinetic materials
				if(materials.get(i).getClass().getName().contains("KineticMaterial")) {
					KineticMaterial material = (KineticMaterial) materials.get(i);
					materialKineticModel.addRow(new Object[] { material.getName(),
							String.valueOf(material.getMass()),
							String.valueOf(material.getCharge()),
							String.valueOf(material.getSpwt0()),
							""
					});
					materialKineticModel.fireTableDataChanged();
				}
				//Load elliptic meshes
				else if(materials.get(i).getClass().getName().contains("SolidMaterial")){
					SolidMaterial material = (SolidMaterial) materials.get(i);
					materialSolidModel.addRow(new Object[] { material.getName(), String.valueOf(material.getMass())});
					materialSolidModel.fireTableDataChanged();
				}
			}

			/*******/
			/*******/
			/** Load Boundary Information and put it in boundary model **/
			/*******/
			/*******/

			ArrayList<Boundary> boundaries = Starfish.boundary_module.getBoundaryList();
			int bsize = boundaries.size();
			for(int i = 0; i < bsize; i++) {
				//Load boundaries
				if(boundaries.get(i).getClass().getName().contains("Boundary")) {
					Boundary boundary = (Boundary) boundaries.get(i);
					boundaryModel.addRow(new Object[] { boundary.getName(),
							boundary.getType().toString(),
							String.valueOf(boundary.getValue())
					});
					boundaryModel.fireTableDataChanged();
				}

			}


			/*******/
			/*******/
			/** TODO: Load Interactions Data **/
			/*******/
			/*******/

			ArrayList<VolumeInteraction> interactions = Starfish.interactions_module.getInteractionsList();
			int isize = interactions.size();
			for(int i = 0; i < isize; i++) {
				//Load chemical interactions
				//{"Process", "Sources", "Products", "Rate", "Rate Coeffs"};
				if(materials.get(i).getClass().getName().contains("ChemicalReaction")) {
					//TODO
				}
				//Load mmc interactions
				else if(materials.get(i).getClass().getName().contains("MMC")){
					//TODO
				}
				//Load dsmc interactions
				else if(materials.get(i).getClass().getName().contains("DSMC")){
					//TODO
				}
				//Load surface interactions
				else if(materials.get(i).getClass().getName().contains("SurfaceInteraction")){
					//TODO
				}
			}

			/*******/
			/*******/
			/** Load Time Data **/
			/*******/
			/*******/

			numField.setText(String.valueOf(Starfish.time_module.getNumIt()));
			dtField.setText(String.valueOf(Starfish.time_module.getDt()));;

			/*******/
			/*******/
			/** TODO: Load Output Data **/
			/*******/
			/*******/

		}
	}

	/**
	 * This method updates the faux output console
	 * @param text
	 * @param outputType
	 */
	private void updateTextArea(final String text, int outputType) {
		//Error/Warning/Exception/Debug Message
		if(outputType == 1) {      
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					try {
						output.getDocument().insertString(output.getDocument().getLength(), text, red);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			});
		} 
		//Standard Message
		else{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					try {
						output.getDocument().insertString(output.getDocument().getLength(), text, black);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	/**
	 * This method changes the printstreams to be our faux output console
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b), 0);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len), 0);
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		System.setOut(new PrintStream(out, true));

		OutputStream err = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b), 1);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len), 1);
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		System.setErr(new PrintStream(err, true));
	}

	/**
	 * Helper method to create red/black colors
	 */
	private void defineSetTypes() {
		black = new SimpleAttributeSet();
		StyleConstants.setForeground(black, Color.BLACK);

		red = new SimpleAttributeSet();
		StyleConstants.setForeground(red, Color.RED);
	}




}