package starfish.gui.builder.form.entry;

import javax.swing.*;

class BoolEntry extends Entry {

    private String tagName;
    private JCheckBox checkBox;

    public BoolEntry(String tagName) {
        this(tagName, false);
    }
    public BoolEntry(String tagName, boolean initialValue) {
        this.tagName = tagName;
        checkBox = new JCheckBox(tagName);
        checkBox.setSelected(initialValue);
        add(checkBox);
    }

    @Override
    public String getValue() {
        return Boolean.toString(checkBox.isSelected());
    }

    @Override
    public String getName() {
        return tagName;
    }

}
