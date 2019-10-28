package starfish.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.SwingConstants;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import javax.swing.JToggleButton;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
					  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
	public GUI() {
		setTitle("Starfish");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 888, 576);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mnFile.add(mntmOpen);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		
		JMenu mnSimulation = new JMenu("Simulation");
		menuBar.add(mnSimulation);
		
		JMenuItem mntmStart = new JMenuItem("Start");
		mnSimulation.add(mntmStart);
		
		JMenuItem mntmPause = new JMenuItem("Pause");
		mnSimulation.add(mntmPause);
		
		JMenuItem mntmStop = new JMenuItem("Stop");
		mnSimulation.add(mntmStop);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmDocumentation = new JMenuItem("Documentation");
		mnHelp.add(mntmDocumentation);
		
		JSeparator separator = new JSeparator();
		mnHelp.add(separator);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		JFrame frame = this;
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(frame, "Starfish Plasma / Rarefied Gas Simulation Code");
			
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
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setOrientation(SwingConstants.VERTICAL);
		toolBar.add(separator_1);
		
		JLabel lblProgress = new JLabel("Progress: ");
		toolBar.add(lblProgress);
		
		JPanel panel = new JPanel();
		toolBar.add(panel);
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(200,16));
		panel.add(progressBar);
		progressBar.setValue(20);
		
		
		JTextArea txtrtestArea = new JTextArea();
		txtrtestArea.setText("(test area)");
		contentPane.add(txtrtestArea, BorderLayout.CENTER);
	}

}
