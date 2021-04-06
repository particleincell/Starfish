package starfish.gui.builder.form.entry;

import starfish.gui.common.FilteredJTextField;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

/**
 * Title
 * Description
 * Text field
 */
public class Entry extends RegularEntry {

    public static class EntryBuilder {

        private JTextField inputComponent;
        private String tagName;
        private String description;

        public EntryBuilder(String tagName) {
            this.tagName = tagName;
            this.description = "";
        }

        public EntryBuilder acceptsInts(int defaultValue) {
            inputComponent = FilteredJTextField.integers(defaultValue);
            return this;
        }
        public EntryBuilder acceptsPositiveInts(int defaultValue) {
            inputComponent = FilteredJTextField.positiveIntegers(defaultValue);
            return this;
        }
        public EntryBuilder acceptsFloats(float defaultValue) {
            inputComponent = FilteredJTextField.rationals(defaultValue);
            return this;
        }
        public EntryBuilder acceptsPositiveFloats(float defaultValue) {
            inputComponent = FilteredJTextField.positiveRationals(defaultValue);
            return this;
        }
        public EntryBuilder acceptsStrings(String defaultValue) {
            inputComponent = new JTextField();
            inputComponent.setText(defaultValue);
            return this;
        }

        public EntryBuilder hasDescription(String description) {
            this.description = description;
            return this;
        }

        public Entry build() {
            if (inputComponent == null) {
                inputComponent = new JTextField();
            }
            return new Entry(tagName, description, inputComponent);
        }

    }

    private String tagName;
    private JTextField textField;

    private Entry(String tagName, String description, JTextField textField) {
        this.tagName = tagName;
        this.textField = textField;
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
