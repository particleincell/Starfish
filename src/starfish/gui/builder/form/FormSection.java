package starfish.gui.builder.form;

import org.w3c.dom.Element;
import starfish.gui.builder.form.entry.Entry;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * List of form elements that lets you add more form elements
 */
public class FormSection extends FormNode {

    private String tagName;
    private boolean allowsChildren;
    private List<Entry> entries;

    public FormSection(String tagName, boolean allowsChildren, List<Entry> entries) {
        this.tagName = tagName;
        this.allowsChildren = allowsChildren;
        this.entries = entries;
        this.setBorder(BorderFactory.createEmptyBorder(0,10,20,20));
        fillContainer(entries);
    }
    private void fillContainer(List<Entry> entries) {
        setLayout(new GridBagLayout());
        setBackground(Color.RED);
        JLabel title = new JLabel(tagName);
        title.setFont(new Font(UIManager.getDefaults().getFont("Label.font").getName(), Font.PLAIN, 28));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        //c.weighty = 1;
        c.gridy = 0;
        add(title, c);

        for (Entry entry : entries) {
            c.gridy += 1;
            add(entry, c);
        }

        c.gridy += 1;
        add(Box.createVerticalGlue(), c);
    }

    @Override
    public Element outputSelfTo(Element parent) {
        Element newSection = parent.getOwnerDocument().createElement(tagName);
        for (Entry node : entries) {
            node.outputSelfTo(newSection);
        }
        parent.appendChild(newSection);
        return newSection;
    }

    @Override
    public String getName() {
        return tagName;
    }

    @Override
    public boolean allowsChildren() {
        return allowsChildren;
    }

}
