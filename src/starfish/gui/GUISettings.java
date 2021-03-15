package starfish.gui;

import starfish.core.common.Options;
import starfish.core.io.LoggerModule;
import starfish.gui.common.FilteredJTextField;
import starfish.gui.viewer.colorconfig.ColorConfigMap;

import javax.swing.*;
import java.awt.*;

public class GUISettings extends JTabbedPane {

    private Options options;

    // General settings
    // These correspond to some of the command line arguments
    private JTextField workingDirectory;
    private JCheckBox randomize;
    private FilteredJTextField maxThreads;
    private JComboBox<String> logLevel;

    public GUISettings(Options options) {
        this.options = options.clone();

        addTab("General", null, createGeneralSettingsPane());
        addTab("Viewer", null, createViewerSettingsPane());
    }
    private JPanel createGeneralSettingsPane() {
        workingDirectory = new JTextField();
        workingDirectory.setText(options.wd);

        randomize = new JCheckBox("Randomize");
        randomize.setToolTipText("If false, the random number generator will be seeded to the same value each time");
        randomize.setSelected(options.randomize);

        String positiveIntRegex = "\\+?\\d*[1-9]";
        maxThreads = new FilteredJTextField(positiveIntRegex);
        maxThreads.setOnFail(s -> JOptionPane.showMessageDialog(this,
                "Max Threads must be positive integer"));
        maxThreads.setToolTipText("Controls (approximately) the maximum number of threads the code will use.");
        maxThreads.setText(Integer.toString(options.max_cores));

        logLevel = new JComboBox<>(new String[] {"info"});
        logLevel.setSelectedItem(LoggerModule.Level.MESSAGE);

        JPanel output = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;

        c.gridy += 1;
        output.add(new JLabel("Working Directory:"), c);
        c.gridy += 1;
        output.add(workingDirectory, c);

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

    public String workingDirectory() {
        return workingDirectory.getText();
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

}
