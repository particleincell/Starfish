package starfish.gui.builder.form.entry;

import starfish.gui.common.FilteredJTextField;

import javax.swing.*;
import java.awt.*;

class FloatEntry extends Entry {

    private String tagName;
    private FilteredJTextField textField;

    public FloatEntry(String tagName, float defaultValue) {
        this.tagName = tagName;
        this.textField = FilteredJTextField.rationals(this, defaultValue);

        setLayout(new GridLayout(2, 1));
        add(new JLabel(tagName));
        add(textField);
    }

    @Override
    public String getValue() {
        return textField.getTrueValue();
    }

    @Override
    public String getName() {
        return tagName;
    }
}
