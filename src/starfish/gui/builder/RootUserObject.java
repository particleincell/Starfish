package starfish.gui.builder;

import javax.swing.*;
import java.awt.*;

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

        JLabel title = new JLabel("Simulation File Builder");
        title.setFont(new Font(UIManager.getDefaults().getFont("Label.font").getName(), Font.PLAIN, 36));
        output.add(title);

        output.add(new JLabel("step 1: I'm not sure either"));

        return output;
    }

}
