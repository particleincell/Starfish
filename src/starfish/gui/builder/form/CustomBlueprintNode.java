package starfish.gui.builder.form;

import org.w3c.dom.Element;
import starfish.gui.builder.form.entry.Entry;

/**
 * Form section that allows the user to create their own entries
 */
public class CustomBlueprintNode extends FormNode {

    private Entry tagName = new Entry.EntryBuilder("XML Tag Name").acceptsStrings("custom_tag_name").build();

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
