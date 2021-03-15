package starfish.gui.builder.form.entry;

import org.w3c.dom.Element;

import javax.swing.*;

/**
 * A terminal form node (content is a string and not more elements)
 */
public abstract class Entry extends JPanel {

    public Element outputSelfTo(Element parent) {
        Element newElement = parent.getOwnerDocument().createElement(getName());
        newElement.setTextContent(getValue());
        parent.appendChild(newElement);
        return newElement;
    }

    public abstract String getName();
    public abstract String getValue();

}
