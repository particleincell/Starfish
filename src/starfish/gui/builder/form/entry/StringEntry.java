package starfish.gui.builder.form.entry;

import javax.swing.*;
import java.awt.*;

public class StringEntry extends RegularEntry {

    private String tagName;
    private JTextField textField;

    public StringEntry(String tagName) {
        this(tagName, "", "");
    }
    public StringEntry(String tagName, String description, String defaultValue) {
        this.tagName = tagName;
        textField = new JTextField();
        textField.setText(defaultValue);
        populate(tagName, description, textField);
    }

    @Override
    public String getValue() {
        return textField.getText();
    }

    @Override
    public String getName() {
        return tagName;
    }

}
