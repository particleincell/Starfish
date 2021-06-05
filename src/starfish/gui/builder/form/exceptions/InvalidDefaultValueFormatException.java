package starfish.gui.builder.form.exceptions;

public class InvalidDefaultValueFormatException extends Exception {

    public InvalidDefaultValueFormatException(String givenValue, String type, String acceptedFormat) {
        super(givenValue + " is not an acceptable default value for type " + type
                + " - must match " + acceptedFormat);
    }

}
