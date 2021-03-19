package starfish.gui;

import starfish.core.common.Options;
import starfish.core.common.Plugin;
import starfish.core.common.Starfish;
import starfish.gui.builder.SimulationFileBuilder;
import starfish.gui.common.GUIUtil;
import starfish.gui.runner.SimulationRunner;
import starfish.gui.viewer.SimulationResultViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;
import java.util.ArrayList;

public class GUI extends JFrame {

    private static GUI gui;
    public static void makeNewGUI(Options options, ArrayList<Plugin> plugins) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                //UIManager.setLookAndFeel("javax.swing.plaf.basic");
            } catch (Exception e) {
                System.out.println("Error setting native LAF: " + e);
            }

            try {
                gui = new GUI(options, plugins);
                gui.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Constants for the card layout
    private final static String BUILD_SIM = "Build Sim File";
    private final static String RUN_SIM = "Run Simulation";
    private final static String VIEW_SIM = "View Results";

    private GUISettings settings;

    private JPanel contentPane; // Side panel and main
    private JPanel main; // Main panel itself
    private SimulationFileBuilder simulationFileBuilder;
    private SimulationRunner simulationRunner;
    private SimulationResultViewer simulationResultViewer;

    // Instances should only be made through GUI.makeNewGUI()
    private GUI(Options options, ArrayList<Plugin> plugins) {
        GUIUtil.setFontScale(2);
        settings = new GUISettings(options);

        applyFrameCustomization();

        simulationFileBuilder = new SimulationFileBuilder();
        simulationRunner = new SimulationRunner(settings, plugins);
        simulationResultViewer = new SimulationResultViewer(simulationRunner);

        try {
            simulationFileBuilder.addCommandsFrom(settings.getSimBuilderBlueprintFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Something went wrong when loading sim builder blueprint file - \n"
                            + e.getMessage()
                            + "\nCommands may be missing");
        }

        createContentPane();
        setContentPane(contentPane);
    }
    private void applyFrameCustomization() {
        URL icon = GUI.class.getResource("/starfish/gui/starfish-100.png");
        setIconImage(Toolkit.getDefaultToolkit().getImage(icon));

        setTitle("Starfish " + Starfish.VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 768, 576);
        setMinimumSize(new Dimension(396, 338));
    }
    private void createContentPane() {
        JPanel sidePanel = createSidePanel();

        main = new JPanel(new CardLayout());
        main.add(simulationFileBuilder, BUILD_SIM);
        main.add(simulationRunner, RUN_SIM);
        main.add(simulationResultViewer, VIEW_SIM);

        contentPane = new JPanel(new BorderLayout());
        contentPane.add(sidePanel, BorderLayout.WEST);
        contentPane.add(main, BorderLayout.CENTER);
    }
    private JPanel createSidePanel() {
        final int WIDTH = 120;
        JPanel sidePanel = new JPanel(new GridBagLayout());
        sidePanel.setMinimumSize(new Dimension(WIDTH, this.getMinimumSize().height));
        sidePanel.setPreferredSize(new Dimension(WIDTH, this.getMinimumSize().height));
        sidePanel.setBackground(Color.LIGHT_GRAY);

        ButtonGroup buttonGroup = new ButtonGroup();

        JToggleButton buildSim = new JToggleButton(BUILD_SIM);
        buildSim.setMnemonic('1');
        buildSim.addActionListener(arg0 -> showSimBuilder());

        JToggleButton runSim = new JToggleButton(RUN_SIM);
        runSim.setMnemonic('2');
        runSim.addActionListener(arg0 -> showSimRunner());

        JToggleButton viewSim = new JToggleButton(VIEW_SIM);
        viewSim.setMnemonic('3');
        viewSim.addActionListener(arg0 -> showSimViewer());

        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(arg0 -> showAboutInformation());

        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(arg0 -> showSettingsWindow());

        for (JToggleButton button : new JToggleButton[] {buildSim, runSim, viewSim}) {
            buttonGroup.add(button);
        }
        for (AbstractButton button : new AbstractButton[] { buildSim, runSim, viewSim, aboutButton, settingsButton }) {
            Dimension dim = new Dimension(WIDTH, WIDTH / 2);
            button.setPreferredSize(dim);
            button.setMinimumSize(dim);
        }

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy += 1;
        sidePanel.add(buildSim, c);
        c.gridy += 1;
        sidePanel.add(runSim, c);
        c.gridy += 1;
        sidePanel.add(viewSim, c);

        c.gridy += 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        sidePanel.add(Box.createVerticalBox(), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;

        c.anchor = GridBagConstraints.PAGE_END;

        c.gridy += 1;
        sidePanel.add(aboutButton, c);

        c.gridy += 1;
        sidePanel.add(settingsButton, c);

        return sidePanel;
    }

    public void showSimBuilder() {
        showOnMainPanel(BUILD_SIM);
    }
    public void showSimRunner() {
        showOnMainPanel(RUN_SIM);
    }
    public void showSimViewer() {
        showOnMainPanel(VIEW_SIM);
    }
    private void showOnMainPanel(String name) {
        CardLayout cl = (CardLayout) main.getLayout();
        cl.show(main, name);
    }

    private static void showAboutInformation() {
        ImageIcon icon = new ImageIcon(GUI.class.getResource("/starfish/gui/starfish-100.png"));
        JOptionPane.showMessageDialog(gui,
                "Starfish Plasma / Rarefied Gas Simulation Code " + Starfish.VERSION + "\n" +
                        "(c) 2012-2019, Particle In Cell Consulting LLC\n" +
                        "info@particleincell.com, www.particleincell.com", "About",
                JOptionPane.INFORMATION_MESSAGE, icon);
    }

    private JDialog dialog;
    private void showSettingsWindow() {
        if (dialog == null) {
            dialog = new JDialog();
            dialog.setTitle("Settings");
            dialog.getContentPane().add(settings);
            dialog.setSize(550, 200);
            dialog.setLocation(this.getX() + this.getWidth() / 2 - dialog.getWidth() / 2,
                    this.getY() + this.getHeight() / 2 - dialog.getHeight() / 2);
            dialog.setMinimumSize(new Dimension(550, 200));
            dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            dialog.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {

                }
            });
        }
        dialog.setVisible(true);
    }

}