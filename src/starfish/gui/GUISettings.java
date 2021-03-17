package starfish.gui;

import starfish.core.common.Options;
import starfish.core.io.LoggerModule;
import starfish.gui.common.FilteredJTextField;
import starfish.gui.common.JTextFileChooserCombo;
import starfish.gui.viewer.colorconfig.ColorConfigMap;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

public class GUISettings extends JTabbedPane {

    private Options options;

    private JTextFileChooserCombo simBuilderChooser;

    // General settings
    // These correspond to some of the command line arguments
    private JCheckBox randomize;
    private FilteredJTextField maxThreads;
    private JComboBox<String> logLevel;

    public GUISettings(Options options) {
        this.options = options.clone();

        addTab("Builder", null, createBuilderSettingsPanel());
        addTab("Simulations", null, createGeneralSettingsPane());
        addTab("Viewer", null, createViewerSettingsPane());
    }
    private JPanel createBuilderSettingsPanel() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        String dir = prefs.get("sim_builder_blueprints", new File("simbuilder.xml").getAbsolutePath());
        simBuilderChooser = new JTextFileChooserCombo(dir, ".xml files", "xml");
        simBuilderChooser.setOnUpdate(file -> {
            prefs.put("sim_builder_blueprints", file.getAbsolutePath());
        });

        JPanel output = new JPanel();
        output.setLayout(new BoxLayout(output, BoxLayout.Y_AXIS));
        output.add(new JLabel("Sim Blueprints Source File"));
        output.add(simBuilderChooser);
        output.add(new JLabel("You will need to restart in order for this change to take effect"));
        return output;
    }
    private JPanel createGeneralSettingsPane() {
        randomize = new JCheckBox("Randomize");
        randomize.setToolTipText("If false, the random number generator will be seeded to the same value each time");
        randomize.setSelected(options.randomize);

        String positiveIntRegex = "\\+?\\d*[1-9]";
        maxThreads = new FilteredJTextField(positiveIntRegex);
        maxThreads.setOnFail(s -> JOptionPane.showMessageDialog(this,
                "Max Threads must be positive integer"));
        maxThreads.setToolTipText("The maximum number of threads the code will use by default.");
        maxThreads.setText(Integer.toString(options.max_cores));

        logLevel = new JComboBox<>(new String[] {"info"});
        logLevel.setSelectedItem(LoggerModule.Level.MESSAGE);

        JPanel output = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;

        c.gridy += 1;
        output.add(randomize, c);

        c.gridy += 1;
        output.add(new JLabel("Max Threads:"), c);
        c.gridy += 1;
        output.add(maxThreads, c);

        c.gridy += 1;
        output.add(new JLabel("Log level:"), c);
        c.gridy += 1;
        output.add(logLevel, c);

        return output;
    }
    private JPanel createViewerSettingsPane() {
        JButton resetPresets = new JButton("Delete all saved color map presets");
        resetPresets.addActionListener(arg0 -> ColorConfigMap.deleteAllSavedPresets());

        JPanel output = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        output.add(resetPresets);

        return output;
    }

    public boolean randomize() {
        return randomize.isSelected();
    }
    public int maxThreads() {
        return Integer.parseInt(maxThreads.getTrueValue());
    }
    public String logLevel() {
        return logLevel.toString();
    }

    public File getSimBuilderBlueprintFile() {
        return simBuilderChooser.getValue();
    }

}
