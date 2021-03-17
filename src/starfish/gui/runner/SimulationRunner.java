package starfish.gui.runner;

import starfish.core.common.Options;
import starfish.core.common.Plugin;
import starfish.core.common.SimStatus;
import starfish.core.common.Starfish;
import starfish.gui.GUI;
import starfish.gui.GUISettings;
import starfish.gui.common.ConsoleOutputStream;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class SimulationRunner extends JPanel {

    private final ArrayList<Plugin> plugins;

    private GUISettings settings;

    private SimQueue simQueue;

    private Starfish sim;
    private Thread simThread;

    private JProgressBar progressBar;
    private GUIConsole console;

    public SimulationRunner(GUISettings settings, ArrayList<Plugin> plugins) {
        super(new BorderLayout());
        this.settings = settings;
        this.plugins = plugins;
        createSidePanel();
        createConsole();
    }
    private void createSidePanel() {
        JButton addFile = new JButton("Add sim to queue");
        addFile.setMnemonic('o');
        addFile.addActionListener(arg0 -> addFileToQueue());

        ButtonGroup buttonGroup = new ButtonGroup();

        JToggleButton start = new JToggleButton("");
        buttonGroup.add(start);
        start.setToolTipText("Start or resume the simulation");
        start.setMnemonic('r');
        start.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/start.png")));
        start.addActionListener(arg0 -> start());

        JToggleButton pause = new JToggleButton("");
        buttonGroup.add(pause);
        pause.setToolTipText("Pause a running simulation");
        pause.setMnemonic('p');
        pause.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/pause.png")));
        pause.addActionListener(arg0 -> pause());

        JToggleButton stop = new JToggleButton("");
        buttonGroup.add(stop);
        stop.setToolTipText("Terminate a running simulation");
        stop.setMnemonic('s');
        stop.setIcon(new ImageIcon(GUI.class.getResource("/starfish/gui/stop.png")));
        stop.addActionListener(arg0 -> stop());

        progressBar = new JProgressBar();
        progressBar.setMaximumSize(new Dimension(100, 16));
        progressBar.setValue(0);

        simQueue = new SimQueue();

        JPanel sidePanel = new JPanel(new GridBagLayout());
        sidePanel.setMinimumSize(new Dimension(200, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy += 1;
        c.gridwidth = 3;
        sidePanel.add(addFile, c);

        c.gridy += 1;
        c.gridwidth = 1;
        sidePanel.add(start, c);
        c.gridx += 1;
        sidePanel.add(pause, c);
        c.gridx += 1;
        sidePanel.add(stop, c);
        c.gridx = 0;
        c.gridwidth = 3;

        c.gridy += 1;
        sidePanel.add(progressBar, c);

        c.gridy += 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        sidePanel.add(simQueue, c);

        add(sidePanel, BorderLayout.WEST);
    }
    private void createConsole() {
        console = new GUIConsole();
        add(console, BorderLayout.CENTER);
    }

    private void addFileToQueue() {
        File file = openStarfishXMLFile();
        if (file != null) {
            Options options = createOptions();
            options.sim_file = file.getName();
            options.wd = file.getParent() + "\\";
            simQueue.enqueue(options);
        }
    }

    public void start() {
        //don't do anything if already running
        SimStatus status;
        if (sim != null) {
            status = sim.getStatus();
        } else {
            status = SimStatus.READY;
        }

        if (status == SimStatus.PAUSED) {
            sim.setStatus(SimStatus.RUNNING);
        } else if (simQueue.enqueuedItemsCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please enqueue items before starting");
        } else if (status == SimStatus.READY || status == SimStatus.STOP) {
            simThread = new Thread(() -> {
                //start the simulation
                console.clear();

                while (simQueue.enqueuedItemsCount() > 0) {
                    sim = new Starfish();
                    sim.start(simQueue.dequeue(), plugins, this);
                }
            });
            simThread.start();
        }
    }
    public void pause() {
        if (sim != null && sim.getStatus() == SimStatus.RUNNING) {
            sim.setStatus(SimStatus.PAUSED);
        }
    }
    public void stop() {
        if (sim != null
                && (sim.getStatus() == SimStatus.RUNNING || sim.getStatus() == SimStatus.PAUSED)) {
            sim.setStatus(SimStatus.STOP);
        }
    }

    public void updateProgress(double progress) {
        progressBar.setValue((int) (progress * 100));
    }

    /**
     * @return The currently running simulation, null if none is running
     */
    public Starfish getSim() {
        return sim;
    }

    public ConsoleOutputStream getConsole() {
        return console;
    }

    private Options createOptions() {
        Options output = new Options();
        output.run_mode = Options.RunMode.GUI_RUN;
        output.randomize = settings.randomize();
        output.max_cores = settings.maxThreads();
        output.log_level = settings.logLevel();
        return output;
    }

    /**
     * @return File of selected file, null if no file is selected
     */
    private File openStarfishXMLFile() {
        UIManager.put("FileChooser.readOnly", Boolean.TRUE); //disable new folder button

        //get last directory
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        String dir = prefs.get("last_dir", FileSystemView.getFileSystemView().getHomeDirectory().toString());

        JFileChooser fileChooser = new JFileChooser(dir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        //fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(".xml files", "xml"));
        fileChooser.setAcceptAllFileFilterUsed(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            prefs.put("last_dir", selectedFile.getParent());  //save the path
            return selectedFile;
        }
        return null;
    }

}
