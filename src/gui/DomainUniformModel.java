package gui;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import starfish.core.common.Starfish;
import starfish.core.domain.UniformMesh;

/**
 *
 * @author Lubos Brieda
 */
@SuppressWarnings("serial")
public class DomainUniformModel extends DefaultTableModel {

    /**
     *
     */
    public DomainUniformModel() {
	}

    /**
     *
     * @param rowCount
     * @param columnCount
     */
    public DomainUniformModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

    /**
     *
     * @param columnNames
     * @param rowCount
     */
    public DomainUniformModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

    /**
     *
     * @param columnNames
     * @param rowCount
     */
    public DomainUniformModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

    /**
     *
     * @param data
     * @param columnNames
     */
    public DomainUniformModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

    /**
     *
     * @param data
     * @param columnNames
     */
    public DomainUniformModel(Object[][] data, Object[] columnNames) {
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
		int nn[] = {Integer.valueOf(rowData[5].toString()), Integer.valueOf(rowData[6].toString())};
		UniformMesh newMesh = new UniformMesh(nn,null,rowData[0].toString(),
				Starfish.domain_module.getDomainType());
		double x0[] = {Double.valueOf(rowData[1].toString()),Double.valueOf(rowData[2].toString())};
		double dh[] = {Double.valueOf(rowData[3].toString()),Double.valueOf(rowData[4].toString())};
		newMesh.setMetrics(x0,dh);
		Starfish.domain_module.addMesh(newMesh);
		
		super.addRow(rowData);
	}
	
    /**
     *
     * @param rowNumber
     */
    public void removeRowChangeStarfish(int rowNumber) {
		Starfish.domain_module.getMeshList().remove(Starfish.domain_module.getMesh(super.getValueAt(rowNumber, 0).toString()));
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
