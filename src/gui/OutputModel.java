package starfish.core.gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;


/**
 *
 * @author Lubos Brieda
 */
@SuppressWarnings("serial")
public class OutputModel extends DefaultTableModel {

    /**
     *
     */
    public OutputModel() {
	}

    /**
     *
     * @param rowCount
     * @param columnCount
     */
    public OutputModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

    /**
     *
     * @param columnNames
     * @param rowCount
     */
    public OutputModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

    /**
     *
     * @param columnNames
     * @param rowCount
     */
    public OutputModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

    /**
     *
     * @param data
     * @param columnNames
     */
    public OutputModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

    /**
     *
     * @param data
     * @param columnNames
     */
    public OutputModel(Object[][] data, Object[] columnNames) {
		super(data, columnNames);
	}

	@Override
    public boolean isCellEditable(int row, int column) {
       //all cells false
       return false;
    }
	
    /**
     *
     * @param rowData
     */
    public void addRowChangeStarfish(Object[] rowData) {
		//Name origin spacing nodes
		//TODO
		
		super.addRow(rowData);
	}
	
    /**
     *
     * @param rowNumber
     */
    public void removeRowChangeStarfish(int rowNumber) {
		//TODO
		super.removeRow(rowNumber);
	}
	
    /**
     *
     */
    public void removeAllRows() {
		int num = super.getRowCount();
		for(int i = num-1; i >= 0; i--) {
			removeRow(i);
		}
	}
}
