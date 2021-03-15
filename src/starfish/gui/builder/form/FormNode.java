package starfish.gui.builder.form;

import org.w3c.dom.Element;

import javax.swing.*;

public abstract class FormNode extends JPanel {

    /**
     * @param parent parent of this node
     * @return the new node created
     */
    public abstract Element outputSelfTo(Element parent);

    public abstract String getName();

    public abstract boolean allowsChildren();

    @Override
    public String toString() {
        return getName();
    }

}
