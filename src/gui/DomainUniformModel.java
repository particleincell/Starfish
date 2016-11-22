package gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule;
import starfish.core.domain.UniformMesh;

@SuppressWarnings("serial")
public class DomainUniformModel extends DefaultTableModel {

	public DomainUniformModel() {
	}

	public DomainUniformModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

	public DomainUniformModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public DomainUniformModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public DomainUniformModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

	public DomainUniformModel(Object[][] data, Object[] columnNames) {
		super(data, columnNames);
	}

	@Override
    public boolean isCellEditable(int row, int column) {
       //all cells false
       return false;
    }
	
	public void addRowChangeStarfish(Object[] rowData) {
		//Name origin spacing nodes
		UniformMesh newMesh = new UniformMesh(Integer.valueOf(rowData[5].toString()), 
				Integer.valueOf(rowData[6].toString()), 
				Starfish.domain_module.getDomainType());
		newMesh.setName(rowData[0].toString());
		newMesh.setOrigin(Double.valueOf(rowData[1].toString()), 
				Double.valueOf(rowData[2].toString()));
		newMesh.setSpacing(Double.valueOf(rowData[3].toString()), 
				Double.valueOf(rowData[4].toString()));
		Starfish.domain_module.addMesh(newMesh);
		
		super.addRow(rowData);
	}
	
	public void removeRowChangeStarfish(int rowNumber) {
		Starfish.domain_module.getMeshList().remove(Starfish.domain_module.getMesh(super.getValueAt(rowNumber, 0).toString()));
		super.removeRow(rowNumber);
	}
	
	public void removeAllRows() {
		int num = super.getRowCount();
		for(int i = num-1; i >= 0; i--) {
			removeRow(i);
		}
	}
}
