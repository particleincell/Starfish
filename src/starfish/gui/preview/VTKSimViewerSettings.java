package starfish.gui.preview;

import vtk.vtkColorSeries;
import vtk.vtkDataArray;
import vtk.vtkLookupTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;

public class VTKSimViewerSettings extends JPanel {

    private static final HashMap<String, ColorMapConfig> configs = new HashMap<>();
    static {

    }

    // Var Color Map editor
    private JComboBox<String> varChooser;
    private JComboBox<ColorScheme> colorChooser;
    private JTextField numBuckets;
    private JTextField min, max;
    private JCheckBox useLogScale;

    private JColorChooser backgroundColorChooser;
    private JCheckBox autoChangeRange;
    private JCheckBox showLegend;

    public VTKSimViewerSettings() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Color Map Settings", createVarColorMapEditor());
        tabbedPane.addTab("Viewer Settings", createGeneralSettings());
        add(tabbedPane);
    }
    private JPanel createVarColorMapEditor() {
        varChooser = new JComboBox<>();
        varChooser.addActionListener(arg0 -> loadValuesIntoMenu(varChooser.getSelectedItem().toString()));

        colorChooser = new JComboBox<>();
        for (ColorScheme scheme : ColorScheme.values()) {
            colorChooser.addItem(scheme);
        }
        colorChooser.addActionListener(arg0 -> updateConfig());

        NumberFormatter positiveIntFormatter = new NumberFormatter(NumberFormat.getInstance());
        positiveIntFormatter.setValueClass(Integer.class);
        positiveIntFormatter.setMinimum(1);
        positiveIntFormatter.setMaximum(Integer.MAX_VALUE);
        positiveIntFormatter.setAllowsInvalid(false);
        positiveIntFormatter.setCommitsOnValidEdit(true);
        numBuckets = new JFormattedTextField(positiveIntFormatter);
        numBuckets.addActionListener(arg0 -> updateConfig());

        NumberFormatter doubleFormatter = new NumberFormatter(NumberFormat.getInstance());
        doubleFormatter.setValueClass(Double.class);
        doubleFormatter.setAllowsInvalid(true);
        doubleFormatter.setCommitsOnValidEdit(true);
        min = new JFormattedTextField(doubleFormatter);
        min.addActionListener(arg0 -> updateConfig());
        max = new JFormattedTextField(doubleFormatter);
        max.addActionListener(arg0 -> updateConfig());

        useLogScale = new JCheckBox("Use logarithmic scale");
        useLogScale.addActionListener(arg0 -> updateConfig());

        GridLayout layout = new GridLayout(4, 2);
        layout.setHgap(15);
        layout.setVgap(15);

        JPanel output = new JPanel(layout);
        output.setBorder(new EmptyBorder(20, 20, 20, 20));
        output.setMinimumSize(new Dimension(300, 300));

        output.add(varChooser);
        output.add(new JLabel());
        output.add(colorChooser);
        output.add(numBuckets);
        output.add(min);
        output.add(max);
        output.add(useLogScale);

        return output;
    }

    /**
     * Updates the config stored in {@code configs} so it matches the user defined config in the GUI
     */
    private void updateConfig() {
        ColorMapConfig config = configs.get(varChooser.getSelectedItem().toString());

        config.setColorScheme((ColorScheme) colorChooser.getSelectedItem());
        config.setNumBuckets(Integer.parseInt(numBuckets.getText()));
        config.setMin(Double.parseDouble(min.getText()));
        config.setMax(Double.parseDouble(max.getText()));
        config.setUseLog(useLogScale.isSelected());

    }
    private JPanel createGeneralSettings() {
        JPanel output = new JPanel();
        output.setLayout(new BoxLayout(output, BoxLayout.Y_AXIS));

        backgroundColorChooser = new JColorChooser();
        backgroundColorChooser.setChooserPanels(new AbstractColorChooserPanel[]{});

        autoChangeRange = new JCheckBox("Automatically change color map range");
        autoChangeRange.setToolTipText("If enabled, the range of the color map of a variable will automatically " +
                "change to the min and max of the new data set whenever a new simulation is loaded");

        showLegend = new JCheckBox("Show color map legend");

        output.add(backgroundColorChooser);
        output.add(autoChangeRange);

        return output;
    }

    /**
     * Adds/changes the configuration of {@code var} to something appropriate for the given {@code vtkDataArray}
     * post condition: {@code var} is a new option in the settings menu
     * @param var
     * @param data data array of the var, if null, the var will be given the default config
     */
    public void putVar(String var, vtkDataArray data) {
        ColorMapConfig.Builder builder = new ColorMapConfig.Builder();

        if (data != null) {
            double[] range = data.GetFiniteRange();
            builder.min(range[0]);
            builder.max(range[1]);
        }

        if (!configs.containsKey(var)) {
            varChooser.addItem(var);
        }
        configs.put(var, builder.build());
    }
    public vtkLookupTable getLookupTable(String var) {
        ColorMapConfig config = configs.getOrDefault(var, new ColorMapConfig.Builder().build());

        // Enum in vtkColorSeries are unavailable, so use this as a reference for values
        // https://vtk.org/doc/nightly/html/classvtkColorSeries.html
        vtkColorSeries colorSeries = new vtkColorSeries();
        colorSeries.SetNumberOfColors(config.getNumBuckets());
        colorSeries.SetColorScheme(config.getColorScheme().getCode());

        vtkLookupTable output = new vtkLookupTable();
        colorSeries.BuildLookupTable(output, 0 /* ORDINAL */);

        output.SetRange(config.getMin(), config.getMax());

        if (config.useLog()) {
            output.SetScaleToLog10();
        }

        return output;
    }

    private void loadValuesIntoMenu(String var) {
        if (!configs.containsKey(var)) {
            putVar(var, null);
        }
        ColorMapConfig config = configs.get(var);
        numBuckets.setText(Integer.toString(config.getNumBuckets()));
        min.setText(Double.toString(config.getMin()));
        max.setText(Double.toString(config.getMax()));
        useLogScale.setSelected(config.useLog());
    }

}
