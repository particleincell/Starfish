package gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import java.awt.BorderLayout;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JButton;
import javax.swing.SpringLayout;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;

public class GUI {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		try {
			  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch(Exception e) {
			  System.out.println("Error setting native LAF: " + e);
			}
		
		frame = new JFrame();
		frame.setBounds(100, 100, 692, 471);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		JMenu mnNewMenu = new JMenu("File");
		menuBar.add(mnNewMenu);
		
		JMenuItem mntmFile = new JMenuItem("New");
		mnNewMenu.add(mntmFile);
		
		JMenuItem mntmNewMenuItem = new JMenuItem("Load");
		mnNewMenu.add(mntmNewMenuItem);
		
		JMenuItem mntmNewMenuItem_1 = new JMenuItem("Save");
		mnNewMenu.add(mntmNewMenuItem_1);
		
		JMenuItem mntmNewMenuItem_2 = new JMenuItem("Save As");
		mnNewMenu.add(mntmNewMenuItem_2);
		
		JMenu mnNewMenu_1 = new JMenu("Run");
		menuBar.add(mnNewMenu_1);
		
		JMenuItem mntmStart = new JMenuItem("Start");
		mnNewMenu_1.add(mntmStart);
		
		JMenuItem mntmPause = new JMenuItem("Pause");
		mnNewMenu_1.add(mntmPause);
		
		JMenuItem mntmExitMainLoop = new JMenuItem("Exit Main Loop");
		mnNewMenu_1.add(mntmExitMainLoop);
		
		JMenuItem mntmStop = new JMenuItem("Stop");
		mnNewMenu_1.add(mntmStop);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmDocumentation = new JMenuItem("Documentation");
		mnHelp.add(mntmDocumentation);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JPanel tabOutput = new JPanel();
		tabbedPane.addTab("Output", null, tabOutput, null);
		tabOutput.setLayout(new BoxLayout(tabOutput, BoxLayout.X_AXIS));
		
		JTextArea textArea = new JTextArea();
		textArea.setText("53543543");
		tabOutput.add(textArea);
		
		JPanel tabMain = new JPanel();
		SpringLayout sl_tabMain = new SpringLayout();
		tabMain.setLayout(sl_tabMain);
		
		JScrollPane scrollPane = new JScrollPane(tabMain);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		tabbedPane.addTab("Main Script", null, scrollPane, null);
		
		JPanel panel = new JPanel();
		sl_tabMain.putConstraint(SpringLayout.NORTH, panel, 25, SpringLayout.NORTH, tabMain);
		sl_tabMain.putConstraint(SpringLayout.WEST, panel, 38, SpringLayout.WEST, tabMain);
		sl_tabMain.putConstraint(SpringLayout.SOUTH, panel, 143, SpringLayout.NORTH, tabMain);
		sl_tabMain.putConstraint(SpringLayout.EAST, panel, 348, SpringLayout.WEST, tabMain);
		tabMain.add(panel);
		
		JCheckBox chckbxNewCheckBox = new JCheckBox("New check box");
		panel.add(chckbxNewCheckBox);
		
		JCheckBox chckbxNewCheckBox_1 = new JCheckBox("New check box");
		panel.add(chckbxNewCheckBox_1);
		
		JComboBox comboBox = new JComboBox();
		panel.add(comboBox);
		
		JPanel panel_1 = new JPanel();
		sl_tabMain.putConstraint(SpringLayout.NORTH, panel_1, 44, SpringLayout.SOUTH, panel);
		sl_tabMain.putConstraint(SpringLayout.WEST, panel_1, 38, SpringLayout.WEST, tabMain);
		sl_tabMain.putConstraint(SpringLayout.SOUTH, panel_1, 162, SpringLayout.SOUTH, panel);
		sl_tabMain.putConstraint(SpringLayout.EAST, panel_1, 348, SpringLayout.WEST, tabMain);
		tabMain.add(panel_1);
		
		JButton btnNewButton = new JButton("New button");
		panel_1.add(btnNewButton);
		
		JSpinner spinner = new JSpinner();
		panel_1.add(spinner);
		
		JPanel tabDomain = new JPanel();
		tabbedPane.addTab("Domain", null, tabDomain, null);
		
		JPanel tabBoundaries = new JPanel();
		tabbedPane.addTab("Boundaries", null, tabBoundaries, null);
		
		JPanel tabMaterials = new JPanel();
		tabbedPane.addTab("Materials", null, tabMaterials, null);
		GridBagLayout gbl_tabMaterials = new GridBagLayout();
		gbl_tabMaterials.columnWidths = new int[]{0};
		gbl_tabMaterials.rowHeights = new int[]{0};
		gbl_tabMaterials.columnWeights = new double[]{Double.MIN_VALUE};
		gbl_tabMaterials.rowWeights = new double[]{Double.MIN_VALUE};
		tabMaterials.setLayout(gbl_tabMaterials);
		
		JPanel tabInteractions = new JPanel();
		tabbedPane.addTab("Interactions", null, tabInteractions, null);
		
		JPanel tabSources = new JPanel();
		tabbedPane.addTab("Sources", null, tabSources, null);
	}
}
