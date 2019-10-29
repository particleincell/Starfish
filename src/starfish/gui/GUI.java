package starfish.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import java.awt.Component;
import java.awt.Desktop;

import javax.swing.Box;
import javax.swing.SwingConstants;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import javax.swing.JToggleButton;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

public class GUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) { 
		GUI.makeNewGUI();
	}
	
	public static void makeNewGUI() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					  UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					//  UIManager.setLookAndFeel("javax.swing.plaf.basic");
					} catch(Exception e) {
					  System.out.println("Error setting native LAF: " + e);
					}
				
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	JFrame frame;
	String sim_file;
	String sim_file_full;
	String sim_file_path;
	
	public GUI() {
		setTitle("Starfish");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 888, 576);
		
		frame = this;		//save a reference to self
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UIManager.put("FileChooser.readOnly", Boolean.TRUE); //disable new folder button
			    // This will define a node in which the preferences can be stored
			    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
			   

			    // First we will get the values
			    // Define a boolean value
			    String dir = prefs.get("last_dir", FileSystemView.getFileSystemView().getHomeDirectory().toString());
			  			    
				JFileChooser fileChooser = new JFileChooser(dir);
		        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				//fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(".xml files", "xml"));
		        fileChooser.setAcceptAllFileFilterUsed(true);
				int result = fileChooser.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION) {
				    File selectedFile = fileChooser.getSelectedFile();
				    sim_file_path = selectedFile.getPath();
				    sim_file_full = selectedFile.getAbsolutePath();
				    sim_file = selectedFile.getName();
				    prefs.put("last_dir", sim_file_path);  //save the path
				    //change name
				    String title_name = sim_file_full;
				    if (title_name.length()>30) 
				    	title_name = "..."+title_name.substring(title_name.length()-30);
				    frame.setTitle(title_name+" - Starfish");
				    
				}
				
			}
		});
		mnFile.add(mntmOpen);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));	
			}
		});
		
		mnFile.add(mntmExit);
		
		JMenu mnSimulation = new JMenu("Simulation");
		menuBar.add(mnSimulation);
		
		JMenuItem mntmRun = new JMenuItem("Run");
		mntmRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mnSimulation.add(mntmRun);
		
		JMenuItem mntmPause = new JMenuItem("Pause");
		mntmPause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
		mnSimulation.add(mntmPause);
		
		JMenuItem mntmStop = new JMenuItem("Stop");
		mntmStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mnSimulation.add(mntmStop);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmDocumentation = new JMenuItem("Documentation");
		mntmDocumentation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		mntmDocumentation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (Desktop.isDesktopSupported()) {
				    try {
				        File myFile = new File(GUI.class.getResource("/starfish/gui/Starfish-UG.pdf").getFile());
				        Desktop.getDesktop().open(myFile);
				} catch (IOException ex) {
				   System.out.println(ex.getMessage());
				}
				}
			}
		});
		mnHelp.add(mntmDocumentation);
		
		JSeparator separator = new JSeparator();
		mnHelp.add(separator);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ImageIcon icon = new ImageIcon(GUI.class.getResource("/starfish/gui/starfish-100.png"));
				JOptionPane.showMessageDialog(frame, "Starfish Plasma / Rarefied Gas Simulation Code "+Starfish.VERSION+"\n"+
													  "(c) 2012-2019, Particle In Cell Consulting LLC\n" + 
													  "info@particleincell.com, www.particleincell.com","About", 
									JOptionPane.INFORMATION_MESSAGE, icon);
			
			}
		});
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		contentPane.add(toolBar, BorderLayout.NORTH);
		
		JToggleButton btnStartButton = new JToggleButton("");
		btnStartButton.setToolTipText("Start or resume the simulation");
		btnStartButton.setMnemonic('r');
		btnStartButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/start.png")));
		toolBar.add(btnStartButton);
		
		JToggleButton btnPauseButton = new JToggleButton("");
		btnPauseButton.setToolTipText("Pause a running simulation");
		btnPauseButton.setMnemonic('p');
		btnPauseButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/pause.png")));
		toolBar.add(btnPauseButton);
		
		JToggleButton btnStopButton = new JToggleButton("");
		btnStopButton.setToolTipText("Terminate a running simulation");
		btnStopButton.setMnemonic('s');
		btnStopButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/stop.png")));
		toolBar.add(btnStopButton);
		
		toolBar.add(new JToolBar.Separator());
		
				
		JLabel lblProgress = new JLabel("Progress: ");
		toolBar.add(lblProgress);
				
		JProgressBar progressBar = new JProgressBar();
		progressBar.setMaximumSize(new Dimension(100,16));
		toolBar.add(progressBar);
		progressBar.setValue(20);
				
		Component horizontalGlue = Box.createHorizontalGlue();
		toolBar.add(horizontalGlue);
		
		JTextArea txtrtestArea = new JTextArea();
		txtrtestArea.setText("(test area)");
		contentPane.add(txtrtestArea, BorderLayout.CENTER);
	}

}
