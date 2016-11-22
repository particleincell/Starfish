package starfish.core.gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule;
import starfish.core.domain.UniformMesh;

@SuppressWarnings("serial")
public class InteractionsMCCModel extends DefaultTableModel {

	public InteractionsMCCModel() {
	}

	public InteractionsMCCModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

	public InteractionsMCCModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public InteractionsMCCModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public InteractionsMCCModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

	public InteractionsMCCModel(Object[][] data, Object[] columnNames) {
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
