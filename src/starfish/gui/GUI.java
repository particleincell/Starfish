package starfish.gui;

import main.Main.Options;
import starfish.core.common.Plugin;
import starfish.core.common.SimStatus;
import starfish.core.common.Starfish;
import starfish.gui.preview.VTKSimViewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class GUI extends JFrame {

    private static GUI gui;

    private Options options;
    private ArrayList<Plugin> plugins;

    public static void makeNewGUI(Options options, ArrayList<Plugin> plugins) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    //UIManager.setLookAndFeel("javax.swing.plaf.basic");
                } catch (Exception e) {
                    System.out.println("Error setting native LAF: " + e);
                }

                try {
                    gui = new GUI(options, plugins);
                    gui.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    gui.sim = null;// delete simulation
                }
            }
        });
    }

    private String sim_file_name;
    private String sim_file_full;
    private String sim_file_path;
    private JProgressBar progressBar;

    private Thread thread;
    private Starfish sim;

    private JPanel contentPane;
    private JSplitPane splitPane;
    private JScrollPane scroll;
    private JTextPane textPane;
    private VTKSimViewer vtkSimViewer;

    private JToggleButton btnStartButton;
    private JToggleButton btnPauseButton;
    private JToggleButton btnStopButton;
    private JToggleButton btnShowSim;

    // Only one instance of GUI should be made through GUI.makeNewGUI()
    private GUI(Options options, ArrayList<Plugin> plugins) {
        URL icon = GUI.class.getResource("/starfish/gui/starfish-100.png");
        setIconImage(Toolkit.getDefaultToolkit().getImage(icon));

        this.options = options.clone();// make our own copy, this will be useful when supporting multiple sims
        this.plugins = plugins;

        setTitle("Starfish " + Starfish.VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 768, 576);

        createMenuBar();

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        createToolBar();

        textPane = new JTextPane();
        textPane.setText("");
        textPane.setAutoscrolls(true);
        scroll = new JScrollPane(textPane);
        // Minimum size needs to be set or else when the VTKSimViewer is shown, the text box will be shrunk to be
        // super thin
        scroll.setMinimumSize(new Dimension(500, 0));

        vtkSimViewer = new VTKSimViewer(sim);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, vtkSimViewer);
        contentPane.add(splitPane, BorderLayout.CENTER);
    }
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmOpen = new JMenuItem("Open");
        mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        mntmOpen.addActionListener(arg0 -> openStarfishXMLFile());
        mnFile.add(mntmOpen);

        JMenuItem mntmExit = new JMenuItem("Exit");
        mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        mntmExit.addActionListener(arg0 -> gui.dispatchEvent(new WindowEvent(gui, WindowEvent.WINDOW_CLOSING)));

        mnFile.add(mntmExit);

        JMenu mnSimulation = new JMenu("Simulation");
        menuBar.add(mnSimulation);

        JMenuItem mntmRun = new JMenuItem("Run");
        mntmRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
        mntmRun.addActionListener(arg0 -> btnStartButton.doClick());
        mnSimulation.add(mntmRun);

        JMenuItem mntmPause = new JMenuItem("Pause");
        mntmPause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
        mntmPause.addActionListener(arg0 -> btnPauseButton.doClick());
        mnSimulation.add(mntmPause);

        JMenuItem mntmStop = new JMenuItem("Stop");
        mntmStop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        mntmStop.addActionListener(arg0 -> btnStopButton.doClick());


        mnSimulation.add(mntmStop);

        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);

        JMenuItem mntmDocumentation = new JMenuItem("Documentation");
        mntmDocumentation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        mntmDocumentation.addActionListener(arg0 -> showDocumentation());
        mnHelp.add(mntmDocumentation);

        JSeparator separator = new JSeparator();
        mnHelp.add(separator);

        JMenuItem mntmAbout = new JMenuItem("About");

        mntmAbout.addActionListener(arg0 -> showAboutInformation());
        mnHelp.add(mntmAbout);
    }
    private void createToolBar() {
        ButtonGroup buttonGroup = new ButtonGroup();

        btnStartButton = new JToggleButton("");
        buttonGroup.add(btnStartButton);
        btnStartButton.setToolTipText("Start or resume the simulation");
        btnStartButton.setMnemonic('r');
        btnStartButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/start.png")));
        btnStartButton.addActionListener(arg0 -> simulationStart());

        btnPauseButton = new JToggleButton("");
        buttonGroup.add(btnPauseButton);
        btnPauseButton.setToolTipText("Pause a running simulation");
        btnPauseButton.setMnemonic('p');
        btnPauseButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/pause.png")));
        btnPauseButton.addActionListener(arg0 -> simulationPause());

        btnStopButton = new JToggleButton("");
        buttonGroup.add(btnStopButton);
        btnStopButton.setToolTipText("Terminate a running simulation");
        btnStopButton.setMnemonic('s');
        btnStopButton.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/stop.png")));
        btnStopButton.addActionListener(arg0 -> simulationStop());


        progressBar = new JProgressBar();
        progressBar.setMaximumSize(new Dimension(100, 16));
        progressBar.setValue(0);


        btnShowSim = new JToggleButton("Hide simulation viewer");
        btnShowSim.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                hideVTKSimViewer();
            } else {
                showVTKSimViewer();
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        contentPane.add(toolBar, BorderLayout.NORTH);

        toolBar.add(btnStartButton);
        toolBar.add(btnPauseButton);
        toolBar.add(btnStopButton);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(new JLabel("Progress: "));
        toolBar.add(progressBar);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnShowSim);

    }


    public void simulationStart() {
        if (sim_file_name == null || sim_file_name.isEmpty()) {
            JOptionPane.showMessageDialog(gui, "Simulation file not selected (use File->Open)");
            return;
        }

        GUI gui = this;
        //don't do anything if already running
        SimStatus status;
        if (sim != null) {
            status = sim.getStatus();
        } else {
            status = SimStatus.READY;
        }

        if (status == SimStatus.READY || status == SimStatus.STOP) {
            thread = new Thread() {
                public void run() {
                    //start the simulation
                    textPane.setText("");
                    options.sim_file = sim_file_name;
                    options.wd = sim_file_path;
                    sim = new Starfish();
                    vtkSimViewer.setSim(sim);
                    sim.start(options, plugins, gui);
                }
            };
            thread.start();
        } else if (status == SimStatus.PAUSED) {
            sim.setStatus(SimStatus.RUNNING);
        }
    }
    public void simulationPause() {
        if (sim != null && sim.getStatus() == SimStatus.RUNNING) {
            sim.setStatus(SimStatus.PAUSED);
        }
    }
    public void simulationStop() {
        if (sim != null
                && sim.getStatus() == SimStatus.RUNNING || sim.getStatus() == SimStatus.PAUSED) {
            sim.setStatus(SimStatus.STOP);
        }
    }


    public void updateProgress(double val) {
        progressBar.setValue((int) (val * 100));
    }

    // per https://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea
    public void printMessage(String msg, Color color) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = textPane.getDocument().getLength();
        textPane.setCaretPosition(len);
        textPane.setCharacterAttributes(aset, false);
        textPane.replaceSelection(msg + "\n");


        //		textArea.append(msg+"\n");
//		textArea.setCaretPosition(textArea.getDocument().getLength());  //scroll to bottom
    }
    public void printMessage(String msg) {
        printMessage(msg, Color.BLACK);
    }
    public void printErrorMessage(String msg) {
        printMessage(msg, Color.RED);
    }

    private void showVTKSimViewer() {
        contentPane.remove(scroll);
        contentPane.add(splitPane);
        contentPane.repaint();
        splitPane.repaint();
    }
    private void hideVTKSimViewer() {
        contentPane.remove(splitPane);
        contentPane.add(scroll);
        contentPane.repaint();
        scroll.repaint();
    }

    private void openStarfishXMLFile() {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE); //disable new folder button

        //get last directory
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        String dir = prefs.get("last_dir", FileSystemView.getFileSystemView().getHomeDirectory().toString());

        JFileChooser fileChooser = new JFileChooser(dir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        //fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(".xml files", "xml"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        int result = fileChooser.showOpenDialog(gui);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            sim_file_full = selectedFile.getAbsolutePath();
            sim_file_path = sim_file_full.substring(0, sim_file_full.lastIndexOf(File.separator) + 1);
            sim_file_name = sim_file_full.substring(sim_file_full.lastIndexOf(File.separator) + 1);
            prefs.put("last_dir", sim_file_path);  //save the path
            //change name
            String title_name = sim_file_full;
            if (title_name.length() > 30)
                title_name = "..." + title_name.substring(title_name.length() - 30);
            gui.setTitle(title_name + " - Starfish");
            textPane.setText("");

        }
    }
    private static void showAboutInformation() {
        ImageIcon icon = new ImageIcon(GUI.class.getResource("/starfish/gui/starfish-100.png"));
        JOptionPane.showMessageDialog(gui,
                "Starfish Plasma / Rarefied Gas Simulation Code " + Starfish.VERSION + "\n" +
                        "(c) 2012-2019, Particle In Cell Consulting LLC\n" +
                        "info@particleincell.com, www.particleincell.com", "About",
                JOptionPane.INFORMATION_MESSAGE, icon);
    }
    private static void showDocumentation() {
        if (Desktop.isDesktopSupported()) {
            try {
                String path = ClassLoader.getSystemClassLoader().getResource(".").getPath() + "../doc/Starfish-UG.pdf";
                URL url = new URL("file://" + path);
                File myFile = new File(url.getPath());
                Desktop.getDesktop().open(myFile);

            } catch (IOException ex) {
                System.err.println(ex.getMessage());

                JOptionPane.showMessageDialog(gui, ex.getMessage());
            }

        }
    }

}