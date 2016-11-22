package starfish.core.gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import starfish.core.common.Starfish;
import starfish.core.domain.DomainModule;
import starfish.core.domain.UniformMesh;
import starfish.core.materials.SolidMaterial;

@SuppressWarnings("serial")
public class MaterialSolidModel extends DefaultTableModel {

	public MaterialSolidModel() {
	}

	public MaterialSolidModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

	public MaterialSolidModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public MaterialSolidModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	public MaterialSolidModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

	public MaterialSolidModel(Object[][] data, Object[] columnNames) {
		super(data, columnNames);
	}

	@Override
    public boolean isCellEditable(int row, int column) {
       //all cells false
       return false;
    }
	
	public void addRowChangeStarfish(Object[] rowData) {
		SolidMaterial material = new SolidMaterial(rowData[0].toString(), Double.valueOf(rowData[1].toString()));
		Starfish.materials_module.getMaterialsList().add(material);
		super.addRow(rowData);
	}
	
	public void removeRowChangeStarfish(int rowNumber) {
		Starfish.materials_module.getMaterialsList().remove(Starfish.materials_module.getMaterial(super.getValueAt(rowNumber, 0).toString()));
		super.removeRow(rowNumber);
	}
	
	public void removeAllRows() {
		int num = super.getRowCount();
		for(int i = num-1; i >= 0; i--) {
			removeRow(i);
		}
	}
}
