package starfish.core.gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule;
import starfish.core.domain.UniformMesh;

@SuppressWarnings("serial")
public class InteractionsDSMCModel extends DefaultTableModel {

	public InteractionsDSMCModel() {
	}

	public InteractionsDSMCModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

	public InteractionsDSMCModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public InteractionsDSMCModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public InteractionsDSMCModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

	public InteractionsDSMCModel(Object[][] data, Object[] columnNames) {
		super(data, columnNames);
	}

	@Override
    public boolean isCellEditable(int row, int column) {
       //all cells false
       return false;
    }
	
	public void addRowChangeStarfish(Object[] rowData) {
		//Name origin spacing nodes
		//TODO
		
		super.addRow(rowData);
	}
	
	public void removeRowChangeStarfish(int rowNumber) {
		//TODO
		super.removeRow(rowNumber);
	}
	
	public void removeAllRows() {
		int num = super.getRowCount();
		for(int i = num-1; i >= 0; i--) {
			removeRow(i);
		}
	}
}
