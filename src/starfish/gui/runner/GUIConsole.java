package starfish.gui.runner;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;

public class GUIConsole extends JScrollPane implements starfish.gui.common.ConsoleOutputStream {

    private JTextPane textPane;

    public GUIConsole() {
        textPane = new JTextPane();
        textPane.setText("");
        textPane.setAutoscrolls(true);
        setViewportView(textPane);
    }

    public void clear() {
        textPane.setText("");
    }
    public void printMessage(String msg) {
        printMessage(msg, Color.BLACK);
    }
    public void printErrorMessage(String msg) {
        printMessage(msg, Color.RED);
    }
    // per https://stackoverflow.com/questions/9650992/how-to-change-text-color-in-the-jtextarea
    public void printMessage(String msg, Color color) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);


        int len = textPane.getDocument().getLength();
        textPane.setCaretPosition(len);
        textPane.setCharacterAttributes(aset, false);
        textPane.replaceSelection(msg + "\n");
    }

}
