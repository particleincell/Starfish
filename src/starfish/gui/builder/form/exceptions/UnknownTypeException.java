package starfish.gui.builder.form.exceptions;

public class UnknownTypeException extends Exception {
    public UnknownTypeException(String type) {
        super("Unknown entry type: " + type);
    }
}
