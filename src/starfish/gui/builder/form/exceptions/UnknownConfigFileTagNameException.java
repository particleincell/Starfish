package starfish.gui.builder.form.exceptions;

import org.w3c.dom.Element;

public class UnknownConfigFileTagNameException extends Exception {

    public UnknownConfigFileTagNameException(Element e) {
        super("the element " + e.getTagName() + " is not a valid starfish builder config tag");
    }

}
