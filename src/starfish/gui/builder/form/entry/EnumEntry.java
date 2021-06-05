package starfish.gui.builder.form.entry;

import javax.swing.*;
import java.awt.*;

public class EnumEntry extends RegularEntry {

    private String tagName;
    private JComboBox<String> jComboBox;

    public EnumEntry(String tagName, String description, String... choices) {
        this.tagName = tagName;
        this.jComboBox = new JComboBox<>(choices);
        populate(tagName, description, jComboBox);

        jComboBox.addActionListener(arg0 -> onValueUpdate());
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
