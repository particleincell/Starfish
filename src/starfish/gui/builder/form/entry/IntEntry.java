package starfish.gui.builder.form.entry;

import starfish.gui.common.FilteredJTextField;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

class IntEntry extends Entry {

    private String tagName;
    private FilteredJTextField textField;

    public IntEntry(String tagName, int defaultValue) {
        this.tagName = tagName;
        this.textField = FilteredJTextField.integers(this, defaultValue);

        setLayout(new GridLayout(2, 1));
        add(new JLabel(tagName));
        add(textField);
    }
    public IntEntry(String tagName, int defaultValue, Predicate<Integer> filter) {
        this.tagName = tagName;
        this.textField = new FilteredJTextField(intToStringPredicate(filter));
        textField.setText(Integer.toString(defaultValue));
        add(textField);
    }
    private static Predicate<String> intToStringPredicate(Predicate<Integer> func) {
        return s -> {
            try {
                return func.test(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return false;
            }
        };
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
