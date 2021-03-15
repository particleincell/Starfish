package starfish.gui.runner;

import starfish.core.common.Options;
import starfish.gui.common.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

class SimQueue extends JPanel {

    private JList<OptionsJListWrapper> jList;
    // I use a Vector because it's the only Collection JLists accept
    private Vector<OptionsJListWrapper> selectionData;

    private JButton remove;

    public SimQueue() {
        super(new BorderLayout());
        selectionData = new Vector<>(8);

        jList = new JList<>();
        jList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jList.addListSelectionListener(arg0 -> listEventAction());
        JScrollPane listScroller = new JScrollPane(jList);
        add(listScroller, BorderLayout.CENTER);

        remove = new JButton("Remove from queue");
        remove.addActionListener(arg0 -> removeFromQueueButtonAction());
        add(remove, BorderLayout.SOUTH);
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
            return GUIUtil.truncateLeft(options.wd + options.sim_file, 47);
        }

    }

}
