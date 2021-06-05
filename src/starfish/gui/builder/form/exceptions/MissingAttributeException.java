package starfish.gui.builder.form.exceptions;

import org.w3c.dom.Element;

public class MissingAttributeException extends Exception {

    public MissingAttributeException(String attributeName, Element e) {
        super("The attribute \"" + attributeName + "\" is missing from element " + e.getTagName());
    }

}
