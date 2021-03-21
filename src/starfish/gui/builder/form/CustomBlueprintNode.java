package starfish.gui.builder.form;

import org.w3c.dom.Element;
import starfish.gui.builder.form.entry.StringEntry;

/**
 * Form section that allows the user to create their own entries
 */
public class CustomBlueprintNode extends FormNode {

    private StringEntry tagName = new StringEntry("XML Tag Name", "", "custom_section");

    public CustomBlueprintNode() {

    }

    @Override
    public Element outputSelfTo(Element parent) {
        return null;
    }

    @Override
    public String getName() {
        return tagName.getValue();
    }

    @Override
    public boolean allowsChildren() {
        return true;
    }

}
