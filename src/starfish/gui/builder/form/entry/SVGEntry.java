package starfish.gui.builder.form.entry;

import starfish.gui.common.FilteredJTextField;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SVGEntry extends Entry {

    private String name;

    private JTextArea textArea;

    public SVGEntry(String name) {
        this.name = name;
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JButton dialogButton = new JButton("Create path");
        dialogButton.addActionListener(arg0 -> showDialog());

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        add(new JLabel(name), c);

        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 1;
        add(scrollPane, c);

        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0;
        add(dialogButton, c);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return textArea.getText();
    }

    private JDialog dialog;
    private void showDialog() {
        if (dialog == null) {
            dialog = new JDialog();
            dialog.setTitle("SVG Generator for \"" + name + "\" variable");
            dialog.getContentPane().add(new SVGEditor(svg -> textArea.setText(svg)));
            dialog.setSize(500, 300);
            dialog.setLocation(this.getX() + this.getWidth() / 2 - dialog.getWidth() / 2,
                    this.getY() + this.getHeight() / 2 - dialog.getHeight() / 2);
            dialog.setMinimumSize(new Dimension(400, 300));
            dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }
        dialog.setVisible(true);
    }

}
class SVGEditor extends JPanel {

    private Consumer<String> onSave;

    // Tab name -> SVG supplier
    private Map<String, Supplier<String>> map;

    /**
     * @param onSave will be given an SVG String when one is made
     */
    public SVGEditor(Consumer<String> onSave) {
        this.onSave = onSave;
        this.map = new HashMap<>();

        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        CircleToSVGEditor circle = new CircleToSVGEditor();
        tabbedPane.addTab("From Circle", circle);
        map.put("From Circle", circle);

        EquationToSVGEditor equation = new EquationToSVGEditor();
        tabbedPane.addTab("From Equation", equation);
        map.put("From Equation", equation);

        PointsToSVGEditor points = new PointsToSVGEditor();
        tabbedPane.addTab("From Points", points);
        map.put("From Points", points);

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(arg0 -> {
            String currentTab = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            String svg = map.get(currentTab).get();
            onSave.accept(svg);
        });
        generateButton.setMinimumSize(new Dimension(500, 0));
        add(generateButton, BorderLayout.SOUTH);
    }

}
class CircleToSVGEditor extends JPanel implements Supplier<String> {

    private FilteredJTextField x, y;
    private FilteredJTextField radius;
    private FilteredJTextField vertices;
    private FilteredJTextField thetaOffset;


    public CircleToSVGEditor() {
        x = FilteredJTextField.positiveRationals(this, 0);
        y = FilteredJTextField.positiveRationals(this, 0);
        radius = FilteredJTextField.positiveRationals(this, 1);
        vertices = FilteredJTextField.positiveIntegers(this, 10);
        thetaOffset = FilteredJTextField.positiveRationals(this, 0);

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;

        c.gridy += 1;
        add(new JLabel("Center coordinate (x, y)"), c);

        c.gridwidth = 1;
        c.weightx = .5;
        c.gridy += 1;
        c.gridx = 0;
        add(x, c);
        c.gridx = 1;
        add(y, c);
        c.gridx = 0;
        c.weightx = 1;

        c.gridwidth = 2;

        c.gridy += 1;
        add(new JLabel("Radius"), c);
        c.gridy += 1;
        add(radius, c);

        c.gridy += 1;
        add(new JLabel("# of vertices"), c);
        c.gridy += 1;
        add(vertices, c);

        c.gridy += 1;
        add(new JLabel("Theta offset (Radians)"), c);
        c.gridy += 1;
        add(thetaOffset, c);

        c.gridy += 1;
        c.weighty = 1;
        add(Box.createVerticalGlue(), c);
    }

    @Override
    public String get() {
        StringBuilder output = new StringBuilder();

        double x0 = Double.parseDouble(x.getTrueValue());
        double y0 = Double.parseDouble(y.getTrueValue());
        double r = Double.parseDouble(radius.getTrueValue());
        int segments = Integer.parseInt(vertices.getTrueValue());
        double theta0 = Double.parseDouble(thetaOffset.getTrueValue());

        double dTheta = (2.0 * Math.PI) / (segments - 1);

        for (int i=0; i < segments; i++) {
            double theta = theta0 + i * dTheta;
            double x = x0 + Math.cos(theta) * r;
            double y = y0 + Math.sin(theta) * r;

            output.append(String.format(" %g,%g",x,y));

            if (i==0) {
                output.append(" L");
            }
        }
        return output.toString();
    }
}
class EquationToSVGEditor extends JScrollPane implements Supplier<String> {

    private FilteredJTextField minT, maxT;
    private FilteredJTextField sections, dT;

    private JTextField x, y;

    public EquationToSVGEditor() {

    }

    @Override
    public String get() {
        return null;
    }

}
class PointsToSVGEditor extends JPanel implements Supplier<String> {

    private static class PointEntry extends JPanel {
        private FilteredJTextField x, y;
        public PointEntry() {
            x = FilteredJTextField.rationals(this, 0);
            y = FilteredJTextField.rationals(this, 0);
            setLayout(new GridLayout(1, 2));
            add(x);
            add(y);
        }
        public float x() {
            return Float.parseFloat(x.getTrueValue());
        }
        public float y() {
            return Float.parseFloat(y.getTrueValue());
        }
    }

    private ArrayList<PointEntry> entries;

    @Override
    public String get() {
        return null;
    }
}
class BezierSplineToSVGEditor extends JPanel implements Supplier<String> {

    @Override
    public String get() {
        return null;
    }

}
