package starfish.gui.builder.form;

import org.w3c.dom.Element;
import starfish.gui.builder.form.entry.Entry;
import starfish.gui.common.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A form with fields
 */
public class FormSection extends FormNode {

    private String tagName;
    private String description;
    private boolean allowsChildren;
    private List<Entry> entries;

    public FormSection(String tagName, String description, boolean allowsChildren, List<Entry> entries) {
        this.tagName = tagName;
        this.description = description;
        this.allowsChildren = allowsChildren;
        this.entries = entries;
        setPreferredSize(new Dimension(0, 0));
        fillContainer(entries);
    }
    private void fillContainer(List<Entry> entries) {
        setLayout(new GridBagLayout());
        JLabel title = new JLabel(GUIUtil.htmlWrap(String.format("<h1>%s</h1><p>%s</p>", tagName, description)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridy = 0;
        add(title, c);

        for (Entry entry : entries) {
            c.gridy += 1;
            add(entry, c);
        }

        c.gridy += 1;
        c.weighty = 1;
        add(Box.createVerticalGlue(), c);
    }

    @Override
    public Element outputSelfTo(Element parent) {
        Element newSection = parent.getOwnerDocument().createElement(tagName);
        for (Entry node : entries) {
            if (!node.getValue().isEmpty()) {
                node.outputSelfTo(newSection);
            }
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
