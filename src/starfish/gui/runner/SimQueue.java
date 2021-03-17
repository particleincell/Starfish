package starfish.gui.runner;

import starfish.core.common.Options;
import starfish.gui.common.FilteredJTextField;
import starfish.gui.common.GUIUtil;
import starfish.gui.common.JTextFileChooserCombo;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;
import java.util.prefs.Preferences;

class SimQueue extends JPanel {

    private JList<OptionsJListWrapper> jList;
    // I use a Vector because it's the only Collection JLists accept
    private Vector<OptionsJListWrapper> selectionData;

    private JButton remove;
    private JButton edit;

    public SimQueue() {
        super(new BorderLayout());
        selectionData = new Vector<>(8);

        jList = new JList<>();
        jList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jList.addListSelectionListener(arg0 -> listEventAction());
        JScrollPane listScroller = new JScrollPane(jList);
        add(listScroller, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 1));
        add(buttons, BorderLayout.SOUTH);

        remove = new JButton("Remove from queue");
        remove.addActionListener(arg0 -> removeFromQueueButtonAction());
        remove.setEnabled(false);
        buttons.add(remove);

        edit = new JButton("Edit");
        edit.addActionListener(arg0 -> editItemAction());
        edit.setEnabled(false);
        buttons.add(edit);
    }

    public void enqueue(Options options) {
        OptionsJListWrapper newItem = new OptionsJListWrapper(options);
        selectionData.add(newItem);
        updateJList();
    }
    public Options dequeue() {
        return removeFromQueue(0);
    }
    public Options peek() {
        return selectionData.size() > 0 ? selectionData.get(0).getOptions() : null;
    }

    public boolean isEmpty() {
        return selectionData.size() == 0;
    }

    public void clear() {
        selectionData.clear();
        updateJList();
    }

    public int enqueuedItemsCount() {
        return selectionData.size();
    }

    private Options removeFromQueue(int index) {
        Options output = null;
        if (selectionData.size() > index) {
            output = selectionData.remove(index).getOptions();
            updateJList();
        }
        return output;
    }

    /**
     * Method that is triggered when the list selection changes.
     */
    private void listEventAction() {
        remove.setEnabled(jList.getSelectedIndices().length > 0);
        edit.setEnabled(jList.getSelectedIndices().length == 1);
    }

    /**
     * Function that is triggered when "Remove from queue" is pressed
     * Removes all the list items that are selected
     */
    private void removeFromQueueButtonAction() {
        int[] indices = jList.getSelectedIndices();
        for (int i = 0; i < indices.length; i++) {
            selectionData.remove(indices[i] - i);
        }
        updateJList();
    }

    /**
     * Function that is triggered when the "Edit" button is pressed
     * Shows a dialog where the user can edit the Options item
     */
    private void editItemAction() {
        int[] indices = jList.getSelectedIndices();
        if (indices.length > 0) {
            int index = indices[0];
            Options options = selectionData.get(index).getOptions();
            OptionsEditorPanel editor = new OptionsEditorPanel(options);
            JDialog dialog = new JDialog();
            dialog.getContentPane().add(editor);
            dialog.setMinimumSize(new Dimension(500, 200));
            dialog.setTitle("Edit simulation settings");
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            dialog.setVisible(true);
        }
        repaint();
    }

    private void updateJList() {
        jList.setListData(selectionData);
    }

    /*
     * The whole point of this class is how the toString method pretty prints the file path. JList uses the toString
     * method to display the object, so instead of using the default Options toString method, I can specify it for
     * this class
     */
    private static class OptionsJListWrapper {

        private final Options options;

        public OptionsJListWrapper(Options options) {
            this.options = options;
        }

        public Options getOptions() {
            return options;
        }

        @Override
        public String toString() {
            return GUIUtil.truncateLeft(options.wd + options.sim_file, 46);
        }

    }

}
class OptionsEditorPanel extends JPanel {

    private Options options;

    public OptionsEditorPanel(Options options) {
        this.options = options;

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy += 1;
        add(new JLabel("Starfish File"), c);
        JTextFileChooserCombo starfishFileChooser = new JTextFileChooserCombo(options.wd + options.sim_file,
                ".xml files", "xml");
        starfishFileChooser.setOnUpdate(file ->  {
            options.sim_file = file.getName();
        });
        c.gridy += 1;
        add(starfishFileChooser, c);


        c.gridy += 1;
        add(new JLabel("Max Threads"), c);
        String positiveIntRegex = "\\+?\\d*[1-9]";
        FilteredJTextField maxThreads = new FilteredJTextField(positiveIntRegex);
        maxThreads.setOnFail(s -> JOptionPane.showMessageDialog(this,
                "Max Threads must be positive integer"));
        maxThreads.setToolTipText("Controls (approximately) the maximum number of threads this simulation instance " +
                "will use.");
        maxThreads.setText(Integer.toString(options.max_cores));
        maxThreads.addActionListener(arg0 -> {
            options.max_cores = Integer.parseInt(maxThreads.getTrueValue());
        });
        c.gridy += 1;
        add(maxThreads, c);


        c.gridy += 1;
        c.weighty = 1;
        Component box = Box.createVerticalGlue();
        box.setMinimumSize(new Dimension(400, 0)); // GridBagLayout shrinks all its contents to their minimum
        // possible size, this this enforces the minimum width
        add(box, c);
    }

}
