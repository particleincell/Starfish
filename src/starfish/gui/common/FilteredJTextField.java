package starfish.gui.common;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilteredJTextField extends JTextField {

    private final static String INT_REGEX = "(\\+|-)?[0-9]+";

    public static FilteredJTextField rationals(float initialValue) {
        String rationalNumberRegex = "(\\+|-)?" + INT_REGEX + "(\\.[0-9]+)?((e|E)" + INT_REGEX + ")?";
        FilteredJTextField output = new FilteredJTextField(rationalNumberRegex);
        output.setText(Float.toString(initialValue));
        output.setOnFail(s -> JOptionPane.showMessageDialog(output, output.getTrueValue() + " is not a number"));
        return output;
    }
    public static FilteredJTextField integers(long initialValue) {
        FilteredJTextField output = new FilteredJTextField(INT_REGEX);
        output.setText(Long.toString(initialValue));
        output.setOnFail(s -> JOptionPane.showMessageDialog(output, output.getTrueValue() + " is not an integer"));
        return output;
    }
    public static FilteredJTextField positiveRationals(float initialValue) {
        String intRegex = "(\\+|-)?[0-9]+";
        String positiveRationalNumberRegex = intRegex + "(\\.[0-9]+)?((e|E)" + intRegex + ")?"; //int or dec or sci. not.
        FilteredJTextField output = new FilteredJTextField(positiveRationalNumberRegex);
        output.setText(Float.toString(initialValue));
        output.setOnFail(s -> JOptionPane.showMessageDialog(output, output.getTrueValue() + " is not a positive rational number"));
        return output;
    }
    public static FilteredJTextField positiveIntegers(long initialValue) {
        String positiveIntRegex = "\\+?[1-9]\\d*";
        FilteredJTextField output = new FilteredJTextField(positiveIntRegex);
        output.setText(Long.toString(initialValue));
        output.setOnFail(s -> JOptionPane.showMessageDialog(output, output.getTrueValue() + " is not a positive integer"));
        return output;
    }

    public static FilteredJTextField integersThatSatisfy(int initialValue,
                                                         Predicate<Integer> filter, String filterDescription) {
        FilteredJTextField output = new FilteredJTextField(intToStringPredicate(filter));
        output.setText(Integer.toString(initialValue));
        output.setOnFail(s -> JOptionPane.showMessageDialog(output, "Must satisfy:\n" + filterDescription));
        return output;
    }

    private static Predicate<String> intToStringPredicate(Predicate<Integer> func) {
        return s -> {
            try {
                return func.test(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }

    private Predicate<String> predicate;

    // Last committed valid entry
    private String lastSuccessfulInput = "";

    public FilteredJTextField(String regex) {
        this(s -> s.matches(regex));
    }
    public FilteredJTextField(Predicate<String> predicate) {
        this.predicate = predicate;
        addActionListener(arg0 -> tryCommitValue());
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                tryCommitValue();
            }
        });
    }

    private Consumer<String> onSuccess;
    public void setOnSuccess(Consumer<String> onSuccess) {
        this.onSuccess = onSuccess;
    }
    private Consumer<String> onFail;
    public void setOnFail(Consumer<String> onFail) {
        this.onFail = onFail;
    }

    /**
     * @return The actual value entered in the text field
     */
    public String getTrueValue() {
        return super.getText();
    }

    /**
     * @return The last value that this Text Field accepts
     */
    @Override
    public String getText() {
        return lastSuccessfulInput;
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        tryCommitValue();
    }

    /**
     * Tests the current text. If it is valid, it will be commited to {@code trueValue}
     */
    private void tryCommitValue() {
        if (predicate.test(getTrueValue())) {
            lastSuccessfulInput = getTrueValue();
            if (onSuccess != null) {
                onSuccess.accept(getTrueValue());
            }
        } else if (onFail != null) {
            onFail.accept(getTrueValue());
        }
    }


}
