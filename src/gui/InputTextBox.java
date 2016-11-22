package starfish.core.gui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class InputTextBox extends JComponent {
	private static final long serialVersionUID = 1L;

	JPanel jpanel;
	JLabel name;
	JTextField field;
	JTextField field2;
	
	public InputTextBox(String newName, int textFieldLength) {
		name = new JLabel(newName);
		field = new JTextField(textFieldLength);
		
		jpanel = new JPanel();
		jpanel.add(name);
		jpanel.add(field);
	}
	
	public InputTextBox(String newName, int textFieldLength, int textFieldLength2) {
		name = new JLabel(newName);
		field = new JTextField(textFieldLength);
		field2 = new JTextField(textFieldLength2);
		
		jpanel = new JPanel();
		jpanel.add(name);
		jpanel.add(field);
		jpanel.add(field2);
	}

	public JPanel getJpanel() {
		return jpanel;
	}

	public void setJpanel(JPanel jpanel) {
		this.jpanel = jpanel;
	}

	public String getName() {
		return name.getText();
	}

	public void setName(String newName) {
		name.setText(newName);
	}

	public String getField() {
		return field.getText();
	}

	public String getField2() {
		return field2.getText();
	}

	
}
