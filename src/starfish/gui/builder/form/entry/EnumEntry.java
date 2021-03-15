package starfish.gui.builder.form.entry;

import javax.swing.*;
import java.awt.*;

public class EnumEntry extends Entry {

    private String tagName;
    private JComboBox<String> jComboBox;

    public EnumEntry(String tagName, String... choices) {
        this.tagName = tagName;
        this.jComboBox = new JComboBox<>(choices);
        setLayout(new GridLayout(2, 1));
        add(new JLabel(tagName));
        add(jComboBox);
    }

    @Override
    public String getValue() {
        return (String) jComboBox.getSelectedItem();
    }

    @Override
    public String getName() {
        return tagName;
    }

}
