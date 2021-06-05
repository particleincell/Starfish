package starfish.gui.common;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Map;
import java.util.prefs.Preferences;

public class RecentFilesSelector extends JPanel {

    private final String KEY;
    private final int SIZE;

    private JList<String> list;
    private Map<String, File> jListItemToFullPath; // truncated file path -> full File

    /**
     * @param key What file is being selected ex: "Sim", "VTK", "Output Dir", ect
     * @param size The maximum number of entries this will "remember"
     */
    public RecentFilesSelector(String key, int size) {
        this.KEY = key;
        this.SIZE = size;

        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        String val = prefs.get(key, "");
        String[] files = val.split(";");

        list = new JList<>(GUIUtil.truncateLeftEach(files, size));
        list.setLayoutOrientation(JList.VERTICAL);
    }

    public void add(File file) {

    }

    public File getSelectedFile() {
        return jListItemToFullPath.get(list.getSelectedValue());
    }

    private ActionEvent onSelect;
    /**
     * Sets an ActionEvent that will be called when an item in this object is selected
     * @param onSelect
     */
    public void onSelect(ActionEvent onSelect) {
        this.onSelect = onSelect;
    }

    /**
     * Saves recent files to Preferences
     */
    private void save() {


        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

    }

}
