package starfish.gui.builder;

import javax.swing.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * FormTreeBuilder will display the JPanel this class creates when the root node of the hierarchy tree is selected.
 * The JPanel contains the documentation on how to use the Sim File Builder
 */
public class RootUserObject {

    public static JPanel create() {
        JPanel output = new JPanel() {
            @Override
            public String toString() {
                return "Root Node";
            }
        };
        output.setLayout(new BoxLayout(output, BoxLayout.Y_AXIS));
        InputStream is = RootUserObject.class.getResourceAsStream("/gui/builder/SimBuilderHowTo.html");
        String htmlString;
        try {
            htmlString = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            htmlString = createErrorHTMLString();
        }

        //JEditorPane editorPane = new JEditorPane();
        //editorPane.setContentType("text/html");
        //editorPane.setText(htmlString);
        //editorPane.setEditable(false);
        //output.add(editorPane);

        //JTextPane textPane = new JTextPane();
        //textPane.setContentType("text/html");
        //textPane.setText(htmlString);
        //output.add(textPane);

        JLabel l = new JLabel(htmlString);
        output.add(l);

        return output;
    }
    private static String createErrorHTMLString() {
        return "bad";
    }

}
