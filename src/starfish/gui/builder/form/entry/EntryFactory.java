package starfish.gui.builder.form.entry;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import starfish.gui.builder.form.exceptions.InvalidDefaultValueFormatException;
import starfish.gui.builder.form.exceptions.UnknownConfigFileTagNameException;
import starfish.gui.builder.form.exceptions.UnknownTypeException;

public class EntryFactory {

    public static Entry makeEntry(Element element) throws UnknownConfigFileTagNameException, UnknownTypeException,
            InvalidDefaultValueFormatException {
        String name = element.getAttribute("name");
        String type = element.getAttribute("type").toLowerCase();
        String defaultValue = element.getAttribute("default");

        Entry output;
        // Types defined in section VI of Starfish user guide
        /*if ("int".equals(type)) {
            if (defaultValue.isEmpty()) {
                defaultValue = "0";
            }
            int initialValue;
            try {
                initialValue = Integer.parseInt(defaultValue);
            } catch (NumberFormatException e) {
                throw new InvalidDefaultValueFormatException(defaultValue, type,
                        "an integer in the range [-2^31, 2^31 - 1]");
            }
            output = new IntEntry(name, initialValue);
        } else if ("int2".equals(type)) {
            output = new Int2Entry(name);
        } else if ("i_list".equals(type)) { // elp
            output = new StringEntry(type);
        } else if ("float".equals(type)) {
            output = new FloatEntry(name);
        } else if ("float2".equals(type)) {
            output = new Float2Entry(name);
        } else if ("f_list".equals(type)) { // elp
            output = new StringEntry(name);*/
        if (type.matches("int|int2|i_list|float|float2|f_list")) {
            output = new StringEntry(name, defaultValue);
        } else if ("bool".equals(type)) {
            if (!defaultValue.matches(DataTypeRegex.BOOL) && !defaultValue.isEmpty()) {
                throw new InvalidDefaultValueFormatException(defaultValue, type, DataTypeRegex.BOOL);
            }
            output = new BoolEntry(name, Boolean.parseBoolean(defaultValue));
        } else if ("string".equals(type)) {
            output = new StringEntry(name);
        } else if ("s_list".equals(type)) { // elp
            output = new StringEntry(name);
        } else if ("s_pairs".equals(type)) { // elp
            output = new StringEntry(name);
        } else if ("s_tuples".equals(type)) { // elp
            output = new StringEntry(name);
        // Custom types
        } else if ("enum".equals(type)) {
            String[] enumValues = getEnumValues(element);
            output = new EnumEntry(name, enumValues);
        } else if ("path".equals(type)) { // elp
            output = new StringEntry(name);
        } else {
            throw new UnknownTypeException(type);
        }
        return output;
    }

    private static boolean isValidEntry(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE || !(node instanceof org.w3c.dom.Element)) {
            return false;
        }
        org.w3c.dom.Element element = (org.w3c.dom.Element) node;
        return element.getTagName().matches("section|entry");
    }

    private static String[] getEnumValues(Element e) {
        NodeList enumNodes = e.getElementsByTagName("enum_choice");
        String[] output = new String[enumNodes.getLength()];
        for (int i = 0; i < enumNodes.getLength(); i++) {
            Node node = enumNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                output[i] = node.getTextContent();
            }
        }
        return output;
    }

}
