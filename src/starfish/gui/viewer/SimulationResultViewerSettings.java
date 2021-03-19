package starfish.gui.viewer;

import starfish.gui.common.FilteredJTextField;
import starfish.gui.viewer.colorconfig.ColorConfig;
import starfish.gui.viewer.colorconfig.ColorConfigMap;
import starfish.gui.viewer.colorconfig.ColorSchemePresets;
import vtk.vtkDataArray;
import vtk.vtkLookupTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Pane that lets user choose color, min, max, etc
 */
class SimulationResultViewerSettings extends JPanel {

    private final ColorConfigMap configs = ColorConfigMap.getInstance();

    // Var Color Map editor
    private JComboBox<String> varChooser;
    private JComboBox<ColorSchemePresets> colorChooser;
    private FilteredJTextField numBuckets;
    private FilteredJTextField min, max;
    private JCheckBox useLogScale;

    private JButton saveConfigForVar;

    public SimulationResultViewerSettings() {
        super(new GridBagLayout());
        initializeFields();
        createLayout();
        setMinimumSize(new Dimension(0, 0));
        setSize(300, getHeight());
    }
    private void initializeFields() {
        colorChooser = new JComboBox<>();
        for (ColorSchemePresets scheme : ColorSchemePresets.values()) {
            colorChooser.addItem(scheme);
        }

        // Default values will be overridden when file loaded, so they don't matter
        numBuckets = FilteredJTextField.positiveIntegers(this, -1);
        min = FilteredJTextField.rationals(this, 0);
        max = FilteredJTextField.rationals(this, 0);

        useLogScale = new JCheckBox("Use logarithmic scale");

        ActionListener onFormUpdate = arg0 -> onFormUpdate();
        // .addActionListener() isn't from an interface or common superclass, so yes, I really do need a separate for
        // loop for each type
        for (JComboBox o : new JComboBox[] { colorChooser }) {
            o.addActionListener(onFormUpdate);
        }
        FocusAdapter onFormUpdateFocusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                onFormUpdate.actionPerformed(null);
            }
        };
        for (JTextField o : new JTextField[] { numBuckets, min, max }) {
            o.addActionListener(onFormUpdate);
            o.addFocusListener(onFormUpdateFocusAdapter); // Commits when user clicks off text area
        }
        for (AbstractButton o : new AbstractButton[] { useLogScale }) {
            o.addActionListener(onFormUpdate);
        }

        varChooser = new JComboBox<>();
        varChooser.addActionListener(arg0 -> {
            Object selectedVar = varChooser.getSelectedItem();
            if (selectedVar != null) {
                loadValuesIntoMenu(configs.get(varChooser.getSelectedItem()));
            }
            if (this.onFormUpdate != null) {
                this.onFormUpdate.actionPerformed(null);
            }
        });

        saveConfigForVar = new JButton("Save as default for var");
        saveConfigForVar.addActionListener(arg0 -> configs.saveToStorage(varChooser.getSelectedItem().toString()));
    }
    private void createLayout() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        // It doesn't seem like the unit for padding is in pixels, but the current val looks OK to my eyes on 1080p
        final int PADDING = 2;
        c.insets = new Insets(PADDING, PADDING, PADDING, PADDING); // Padding

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;

        // Var
        c.gridy += 1;
        add(new JLabel("Select var:"), c);
        c.gridy += 1;
        add(varChooser, c);

        // Title
        JLabel title = new JLabel("Color Map Settings:");
        title.setFont(new Font(UIManager.getDefaults().getFont("Label.font").getName(), Font.BOLD, 18));
        c.gridy += 1;
        add(title, c);

        // Color
        c.gridy += 1;
        add(new JLabel("Select color:"), c);
        c.gridy += 1;
        add(colorChooser, c);

        // Buckets
        c.gridy += 1;
        add(new JLabel("Number of buckets:"), c);
        c.gridy += 1;
        add(numBuckets, c);

        // min/max
        c.gridy += 1;
        c.gridwidth = 1;
        c.weightx = .5;

        c.gridx = 0;
        add(new JLabel("min:"), c);
        c.gridx = 1;
        add(new JLabel("max:"), c);

        c.gridy += 1;

        c.gridx = 0;
        add(min, c);
        c.gridx = 1;
        add(max, c);

        c.gridwidth = 2;
        c.weightx = 1;
        c.gridx = 0;

        // Log scale
        c.gridy += 1;
        add(useLogScale, c);

        c.gridy += 1;
        c.weighty = 1;
        add(Box.createVerticalBox(), c);
        c.weighty = 0;

        c.anchor = GridBagConstraints.PAGE_END;

        c.gridy += 1;
        add(saveConfigForVar, c);
    }

    /**
     * Adds/changes the configuration of {@code var} to something appropriate for the given {@code vtkDataArray}
     * @param var
     * @param data data array of the var, if null, the var will be given the default config
     */
    public void putVar(String var, vtkDataArray data) {
        ColorConfig config = configs.getOrDefault(var, new ColorConfig.Builder().build());

        if (data != null) {
            double[] range = data.GetFiniteRange();
            config.setMin(range[0]);
            config.setMax(range[1]);
        }

        configs.putIfAbsent(var, config);
    }

    public boolean varIsRegistered(String var) {
        return configs.containsKey(var);
    }

    public vtkLookupTable getLookupTable(String var) {
        ColorConfig config = configs.get(var);
        return config == null ? null : config.buildLookupTable();
    }

    /**
     * @param vars List of names of vars that the user will be able to select. Won't include vars not registered
     */
    public void setActiveVars(String... vars) {
        Object previouslyActiveVar = varChooser.getSelectedItem();
        varChooser.removeAllItems();
        for (String var : vars) {
            if (configs.containsKey(var)) {
                varChooser.addItem(var);
            }
        }
        if (previouslyActiveVar != null) {
            varChooser.setSelectedItem(previouslyActiveVar);
        }
    }

    public String getSelectedVar() {
        return varChooser.getSelectedItem() == null ? null : (String) varChooser.getSelectedItem();
    }

    private ActionListener onFormUpdate;
    public void setOnFormUpdate(ActionListener a) {
        this.onFormUpdate = a;
    }
    private void onFormUpdate() {
        updateConfig();
        if (onFormUpdate != null) {
            onFormUpdate.actionPerformed(null);
        }
    }
    /**
     * Updates the config stored in {@code configs} so it matches the user defined config in the GUI
     */
    private void updateConfig() {
        Object selectedItem = varChooser.getSelectedItem();
        if (selectedItem != null) {
            ColorConfig config = configs.get(selectedItem.toString());
            if (config != null) {
                if (colorChooser.getSelectedItem() != null) {
                    config.setColorScheme((ColorSchemePresets) colorChooser.getSelectedItem());
                }
                config.setNumBuckets(Integer.parseInt(numBuckets.getTrueValue()));
                config.setMin(Double.parseDouble(min.getTrueValue()));
                config.setMax(Double.parseDouble(max.getTrueValue()));
                config.setUseLog(useLogScale.isSelected());
            }
        }
    }

    private void loadValuesIntoMenu(ColorConfig config) {
        numBuckets.setText(Integer.toString(config.getNumBuckets()));
        min.setText(Double.toString(config.getMin()));
        max.setText(Double.toString(config.getMax()));
        useLogScale.setSelected(config.useLog());
        // For some reason, you need to make this last or else the form values don't update
        colorChooser.setSelectedItem(config.getColorScheme());
    }
}
