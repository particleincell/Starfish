package starfish.gui.builder.form.entry;

import starfish.gui.common.GUIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * If an entry follows this format:
 * [title]
 * [description]
 * [some component where they do the input]
 * By extending this class, entries can do this format easier.
 *
 * Most of the types of entries follow this format, hence why this is considered a "Regular" entry
 */
public abstract class RegularEntry extends Entry {

    /**
     * Side effect: This removes all child components before populating itself.
     */
    protected void populate(String title, String description, Component field) {
        removeAll();
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        String htmlString = String.format("<h3>%s</h3><p>%s</p>", title, description);
        JLabel label = new JLabel(GUIUtil.htmlWrap(htmlString));

        for (Component component : new Component[] {label, field}) {
            c.gridy += 1;
            add(component, c);
        }
    }

}
