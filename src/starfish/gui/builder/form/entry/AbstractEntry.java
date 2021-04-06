package starfish.gui.builder.form.entry;

import org.w3c.dom.Element;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * A terminal form node (content is a string and not more elements)
 */
public abstract class AbstractEntry extends JPanel {

    private boolean enabled = true;
    private String conditionVar, conditionVal;

    /**
     * @return New element created, null if none created or if entry is disabled because its condition is not met
     */
    public Element outputSelfTo(Element parent) {
        if (enabled) {
            Element newElement = parent.getOwnerDocument().createElement(getName());
            newElement.setTextContent(getValue());
            parent.appendChild(newElement);
            return newElement;
        } else {
            return null;
        }
    }

    public void setCondition(String condition) {
        String[] arr = condition.split("/");
        String var = arr[0];
        String val = arr[1];
        this.conditionVar = var;
        this.conditionVal = val;
    }
    public void updateCondition(String var, String val) {
        if (conditionVar != null && var.equals(conditionVar)) {
            enabled = val.matches(conditionVal);
            setVisible(enabled);
        }

    }

    private Consumer<String[]> listener;
    public void setValueListener(Consumer<String[]> listener) {
        this.listener = listener;
    }

    /**
     * Child classes have an obligation to call this when their value is updated
     */
    public void onValueUpdate() {
        listener.accept(new String[] {getName(), getValue()});
    }

    public abstract String getName();
    public abstract String getValue();

}
