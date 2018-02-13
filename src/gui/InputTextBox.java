package starfish.core.gui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Lubos Brieda
 */
public class InputTextBox extends JComponent {
	private static final long serialVersionUID = 1L;

	JPanel jpanel;
	JLabel name;
	JTextField field;
	JTextField field2;
	
    /**
     *
     * @param newName
     * @param textFieldLength
     */
    public InputTextBox(String newName, int textFieldLength) {
		name = new JLabel(newName);
		field = new JTextField(textFieldLength);
		
		jpanel = new JPanel();
		jpanel.add(name);
		jpanel.add(field);
	}
	
    /**
     *
     * @param newName
     * @param textFieldLength
     * @param textFieldLength2
     */
    public InputTextBox(String newName, int textFieldLength, int textFieldLength2) {
		name = new JLabel(newName);
		field = new JTextField(textFieldLength);
		field2 = new JTextField(textFieldLength2);
		
		jpanel = new JPanel();
		jpanel.add(name);
		jpanel.add(field);
		jpanel.add(field2);
	}

    /**
     *
     * @return
     */
    public JPanel getJpanel() {
		return jpanel;
	}

    /**
     *
     * @param jpanel
     */
    public void setJpanel(JPanel jpanel) {
		this.jpanel = jpanel;
	}

	public String getName() {
		return name.getText();
	}

	public void setName(String newName) {
		name.setText(newName);
	}

    /**
     *
     * @return
     */
    public String getField() {
		return field.getText();
	}

    /**
     *
     * @return
     */
    public String getField2() {
		return field2.getText();
	}

	
}
