package starfish.gui.builder.form.entry;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import starfish.gui.builder.form.exceptions.InvalidDefaultValueFormatException;
import starfish.gui.builder.form.exceptions.UnknownConfigFileTagNameException;
import starfish.gui.builder.form.exceptions.UnknownTypeException;

public class EntryFactory {

    public static AbstractEntry makeEntry(Element element) throws UnknownConfigFileTagNameException, UnknownTypeException,
            InvalidDefaultValueFormatException {
        String name = element.getAttribute("name");
        String description = element.getAttribute("description");
        String type = element.getAttribute("type").toLowerCase();
        String defaultValue = element.getAttribute("default");

        AbstractEntry output;
        // Types defined in section VI of Starfish user guide
        if ("int".equals(type)) {
            if (defaultValue.isEmpty()) {
                defaultValue = "0";
            }
            int parsedDefaultValue;
            try {
                parsedDefaultValue = Integer.parseInt(defaultValue);
            } catch (NumberFormatException e) {
                throw new InvalidDefaultValueFormatException(defaultValue, type,
                        "An integer in the range [-2^31, 2^31 - 1]");
            }
            output = new Entry.EntryBuilder(name)
                    .acceptsInts(parsedDefaultValue)
                    .hasDescription(description)
                    .build();
        }/* else if ("int2".equals(type)) {
        } else if ("i_list".equals(type)) {
        }*/ else if ("float".equals(type)) {
            if (defaultValue.isEmpty()) {
                defaultValue = "0";
            }
            float parsedDefaultValue;
            try {
                parsedDefaultValue = Float.parseFloat(defaultValue);
            } catch (NumberFormatException e) {
                throw new InvalidDefaultValueFormatException(defaultValue, type, "A rational number");
            }
            output = new Entry.EntryBuilder(name)
                    .acceptsFloats(parsedDefaultValue)
                    .hasDescription(description)
                    .build();
        } /*else if ("float2".equals(type)) {
        } else if ("f_list".equals(type)) { */
        else if (type.matches("int2|int3|i_list|float2|float3|f_list")) {
            output = new Entry.EntryBuilder(name)
                    .acceptsStrings(defaultValue)
                    .hasDescription(description)
                    .build();
        } else if ("bool".equals(type)) {
            final String BOOL_REGEX = "(?i)true|false";
            if (!defaultValue.matches(BOOL_REGEX) && !defaultValue.isEmpty()) {
                throw new InvalidDefaultValueFormatException(defaultValue, type, BOOL_REGEX);
            }
            output = new BoolEntry(name, Boolean.parseBoolean(defaultValue));
        } else if ("string".equals(type)) {
            output = new Entry.EntryBuilder(name)
                    .acceptsStrings(defaultValue)
                    .hasDescription(description)
                    .build();
        } /*else if ("s_list".equals(type)) {
        } else if ("s_pairs".equals(type)) {
        } else if ("s_tuples".equals(type)) {*/
        else if (type.matches("s_list|s_pairs|s_tuples")) {
            output = new Entry.EntryBuilder(name)
                    .acceptsStrings(defaultValue)
                    .hasDescription(description)
                    .build();
        // Custom types
        } else if ("enum".equals(type)) {
            String[] enumValues = getEnumValues(element);
            output = new EnumEntry(name, description, enumValues);
        } else if ("path".equals(type)) {
            output = new SVGEntry(name);
        } else {
            throw new UnknownTypeException(type);
        }

        String condition = element.getAttribute("condition");
        if (!condition.isBlank()) {
            output.setCondition(condition);
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
