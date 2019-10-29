package starfish.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import starfish.core.common.Plugin;
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
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import javax.swing.JToggleButton;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.ButtonGroup;

public class GUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	String args[];
	ArrayList<Plugin> plugins;
	static GUI gui;
		
	public static void makeNewGUI(String args[], ArrayList<Plugin> plugins) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					  UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					//  UIManager.setLookAndFeel("javax.swing.plaf.basic");
					} catch(Exception e) {
					  System.out.println("Error setting native LAF: " + e);
					}
				
				try {
					gui = new GUI(args, plugins);
					gui.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
					gui.sim = null;		//delete simulation
					
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	JFrame frame;
	String sim_file_name;
	String sim_file_full;
	String sim_file_path;
	JProgressBar progressBar;
	public enum SimStatus {READY, RUNNING, PAUSED, STOP};
	Thread thread;
	Starfish sim;
	JTextPane textPane;
	JToggleButton btnStartButton;
	JToggleButton btnPauseButton;
	JToggleButton btnStopButton;
	
	public GUI(String args[], ArrayList<Plugin> plugins) {
		
		this.args = args;
		this.plugins = plugins;
		
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
			    
				//get last directory
			    Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
			    String dir = prefs.get("last_dir", FileSystemView.getFileSystemView().getHomeDirectory().toString());
			  			    
				JFileChooser fileChooser = new JFileChooser(dir);
		        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				//fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(".xml files", "xml"));
		        fileChooser.setAcceptAllFileFilterUsed(true);
				int result = fileChooser.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION) {
				    File selectedFile = fileChooser.getSelectedFile();
				    sim_file_full = selectedFile.getAbsolutePath();
				    sim_file_path =  sim_file_full.substring(0,sim_file_full.lastIndexOf(File.separator)+1);
				    sim_file_name = sim_file_full.substring(sim_file_full.lastIndexOf(File.separator)+1);
				    prefs.put("last_dir", sim_file_path);  //save the path
				    //change name
				    String title_name = sim_file_full;
				    if (title_name.length()>30) 
				    	title_name = "..."+title_name.substring(title_name.length()-30);
				    frame.setTitle(title_name+" - Starfish");
				    textPane.setText("");
				    
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
		mntmRun.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnStartButton.doClick();
			}			
		});
		mnSimulation.add(mntmRun);
		
		JMenuItem mntmPause = new JMenuItem("Pause");
		mntmPause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
		mntmPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnPauseButton.doClick();
			}			
		});
		mnSimulation.add(mntmPause);
		
		JMenuItem mntmStop = new JMenuItem("Stop");
		mntmStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnStopButton.doClick();
			}			
		});

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
		
		ButtonGroup buttonGroup = new ButtonGroup();

		btnStartButton = new JToggleButton("");
		buttonGroup.add(btnStartButton);
		btnStartButton.setToolTipText("Start or resume the simulation");
		btnStartButton.setMnemonic('r');
		btnStartButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/start.png")));
		btnStartButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				simulationStart();
			}
		});
		toolBar.add(btnStartButton);
		
	    btnPauseButton = new JToggleButton("");
		buttonGroup.add(btnPauseButton);
		btnPauseButton.setToolTipText("Pause a running simulation");
		btnPauseButton.setMnemonic('p');
		btnPauseButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/pause.png")));
		btnPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				simulationPause();
			}
		});
		toolBar.add(btnPauseButton);
		
		btnStopButton = new JToggleButton("");
		buttonGroup.add(btnStopButton);
		btnStopButton.setToolTipText("Terminate a running simulation");
		btnStopButton.setMnemonic('s');
		btnStopButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/stop.png")));
		btnStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				simulationStop();
			}
		});
		toolBar.add(btnStopButton);
		
		toolBar.add(new JToolBar.Separator());
		
				
		JLabel lblProgress = new JLabel("Progress: ");
		toolBar.add(lblProgress);
				
	    progressBar = new JProgressBar();
		progressBar.setMaximumSize(new Dimension(100,16));
		toolBar.add(progressBar);
		progressBar.setValue(0);
				
		Component horizontalGlue = Box.createHorizontalGlue();
		toolBar.add(horizontalGlue);
		
		textPane = new JTextPane();
		textPane.setText("");
		textPane.setAutoscrolls(true);
	    JScrollPane scroll = new JScrollPane (textPane);
		contentPane.add(scroll, BorderLayout.CENTER);
	}
	

	
	public void simulationStart() {
		if (sim_file_name==null || sim_file_name.isEmpty()) 
		{
			JOptionPane.showMessageDialog(frame, "Simulation file not selected (use File->Open)");
			return;
		}
		
		GUI gui = this;
		//don't do anything if already running
		SimStatus status;
		if (sim!=null) status = sim.getStatus(); else status=SimStatus.READY;
		
		switch (status) {
		case READY:
		case STOP:
			thread = new Thread() {
			    public void run() {
			        	//start the simulation
			    		textPane.setText("");
				    	sim = new Starfish();
				    	sim.setSimFile(sim_file_path,sim_file_name);
				    	sim.start(args, plugins,gui);
                }  
			};

			thread.start();
			break;
			
		case RUNNING:
			return;		//don't do anything if already running
		case PAUSED:
			sim.setStatus(SimStatus.RUNNING);
			break;
		default:
				break;
		}
	}
	

	public void simulationPause() {
		if (sim==null) return;
		
		switch (sim.getStatus())
		{
		case RUNNING:
			sim.setStatus(SimStatus.PAUSED);
			
			break;
		default: break;	//don't do anything unless running
		
		}
	}

	public void simulationStop() {
		
		if (sim==null) return;
		
		switch (sim.getStatus())
		{
		case RUNNING:
		case PAUSED:
			sim.setStatus(SimStatus.STOP);
			
			break;
		default: break;	//don't do anything unless running
		
		}
		
	}
	
	public void updateProgress(double val) {
		progressBar.setValue((int)(val*100));
		
	}
	
	//per https://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea
	public void printMessage(String msg, Color color) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = textPane.getDocument().getLength();
	    textPane.setCaretPosition(len);
        textPane.setCharacterAttributes(aset, false);
        textPane.replaceSelection(msg+"\n");

        
		//		textArea.append(msg+"\n");
//		textArea.setCaretPosition(textArea.getDocument().getLength());  //scroll to bottom
	}
	
	public void printMessage(String msg)
	{
		printMessage(msg,Color.BLACK);
	}
	public void printErrorMessage(String msg) {
		printMessage(msg,Color.RED);
	}


}
