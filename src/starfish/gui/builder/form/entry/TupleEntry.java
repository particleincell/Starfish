package starfish.gui.builder.form.entry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TupleEntry<T extends Entry> extends Entry {

    private String tagName;
    private int elements;
    private String delimiter;

    private List<T> elem;

    /**
     * @param supplier supplies new instances of T
     * @param elements number of elements in this tuple. elements=2 -> x, y, elements=3 -> x,y,z, etc
     */
    public TupleEntry(String tagName, Supplier<T> supplier, int elements) {
        this(tagName, supplier, elements, ",");
    }

    /**
     * @param supplier supplies new instances of T
     * @param elements number of elements in this tuple. elements=2 -> x,y , elements=3 -> x,y,z, etc
     * @param delimiter delimiter used in the output. ex: getValueAsString() returns
     *                  [entry1Value][delimiter][entry2Value][delimiter[entry3Value]
     */
    public TupleEntry(String tagName, Supplier<T> supplier, int elements, String delimiter) {
        if (elements < 1) {
            throw new IllegalArgumentException("elements can't be " + elements + ". # of elements must be 1 or more");
        }
        this.tagName = tagName;
        this.elements = elements;
        this.delimiter = delimiter;
        this.elem = new ArrayList<>(elements);

        setLayout(new GridLayout(2, 1));
        add(new JLabel(tagName));
        add(createTextFieldRow(supplier, elements));
    }
    private JPanel createTextFieldRow(Supplier<T> supplier, int num) {
        JPanel output = new JPanel(new GridLayout(1, num));
        for (int i = 0; i < num; i++) {
            T newEntry = supplier.get();
            output.add(newEntry);
            elem.add(newEntry);
        }
        return output;
    }

    @Override
    public String getValue() {
        StringBuilder output = new StringBuilder();
        for (T e : elem) {
            output.append(e.getValue());
            output.append(delimiter);
        }
        if (elem.size() >= 1) {
            output.delete(output.length() - delimiter.length(), output.length());
        }
        return output.toString();
    }

    @Override
    public String getName() {
        return tagName;
    }

    public int getNumberOfElements() {
        return elements;
    }

    /**
     * @return The delimiter used to join entry values in the output
     */
    public String getDelimiter() {
        return delimiter;
    }
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

}
