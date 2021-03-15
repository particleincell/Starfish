package starfish.gui.builder.form;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import starfish.gui.builder.form.entry.Entry;
import starfish.gui.builder.form.entry.EntryFactory;
import starfish.gui.builder.form.exceptions.InvalidDefaultValueFormatException;
import starfish.gui.builder.form.exceptions.UnknownConfigFileTagNameException;
import starfish.gui.builder.form.exceptions.UnknownTypeException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FormNodeFactory {

    public static Supplier<FormNode> makeNodeSupplier(Element element)  {
        return () -> {
            try {
                return FormNodeFactory.makeNode(element);
            } catch (Exception e) {
                return null;
            }
        };
    }
    public static FormNode makeNode(Element element) throws UnknownConfigFileTagNameException, UnknownTypeException,
            InvalidDefaultValueFormatException {
        FormNode output;
        String tagName = element.getTagName().toLowerCase();

        if ("blueprint".equals(tagName)) {
            List<Entry> sectionChildren = getEntries(element.getChildNodes());
            output = new FormSection(element.getAttribute("name"),
                    Boolean.parseBoolean(element.getAttribute("allows_children")), sectionChildren);
        } else {
            throw new UnknownConfigFileTagNameException(element);
        }

        return output;
    }
    private static List<Entry> getEntries(NodeList nodeList) throws UnknownConfigFileTagNameException,
            UnknownTypeException, InvalidDefaultValueFormatException {
        List<Entry> output = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Entry newFormChild = EntryFactory.makeEntry((Element) childNode);
                output.add(newFormChild);
            }
        }
        return output;
    }

}
