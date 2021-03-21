package starfish.gui.builder.form.entry;

import starfish.gui.common.FilteredJTextField;

import javax.swing.*;
import java.awt.*;

class FloatEntry extends RegularEntry {

    private String tagName;
    private FilteredJTextField textField;

    public FloatEntry(String tagName, String description, float defaultValue) {
        this.tagName = tagName;
        this.textField = FilteredJTextField.rationals(this, defaultValue);
        populate(tagName, description, textField);
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
