package starfish.gui.builder.form.entry;

import starfish.gui.common.FilteredJTextField;

import javax.swing.*;

class FloatEntry extends Entry {

    private String tagName;
    private JTextField textField;

    public FloatEntry(String tagName) {
        this.tagName = tagName;
        this.textField = new JTextField();
        add(new JLabel(tagName));
        add(textField);
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getName() {
        return tagName;
    }
}
