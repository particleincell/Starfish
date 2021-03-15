package starfish.gui.builder.form.entry;

import javax.swing.*;
import java.awt.*;

public class SVGEntry extends Entry {

    private JComboBox<Type> type;

    public SVGEntry(String name) {
        type = new JComboBox<>(Type.values());

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridy += 1;
        add(new JLabel(name));


    }


    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    private enum Type {
        LINE,
        CIRCLE,
        SPLINE,
        CUSTOM
    }

}
