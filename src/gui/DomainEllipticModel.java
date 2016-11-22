package starfish.core.gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule;
import starfish.core.domain.UniformMesh;

@SuppressWarnings("serial")
public class DomainEllipticModel extends DefaultTableModel {


	public DomainEllipticModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

	public DomainEllipticModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public DomainEllipticModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public DomainEllipticModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

	public DomainEllipticModel(Object[][] data, Object[] columnNames) {
		super(data, columnNames);
	}

	@Override
    public boolean isCellEditable(int row, int column) {
       //all cells false
       return false;
    }
	
	public void addRowChangeStarfish(Object[] rowData) {
		//TODO: Change to Specs to add elliptic Mesh

		super.addRow(rowData);
	}
	
	@Override
	public void removeRow(int rowNumber) {
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
