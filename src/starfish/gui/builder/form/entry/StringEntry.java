package starfish.gui.builder.form.entry;

import javax.swing.*;
import java.awt.*;

public class StringEntry extends Entry {

    private String tagName;
    private JTextField textField;

    public StringEntry(String tagName) {
        this(tagName, "");
    }
    public StringEntry(String tagName, String defaultValue) {
        setLayout(new GridLayout(2, 1));
        this.tagName = tagName;
        textField = new JTextField();
        textField.setText(defaultValue);
        add(new JLabel(tagName));
        add(textField);
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
