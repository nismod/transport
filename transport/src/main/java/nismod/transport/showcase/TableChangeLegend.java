package nismod.transport.showcase;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.geotools.brewer.color.ColorBrewer;

/**
 * Table change legend (vertical) to include in the dashboards.
 * @author Milan Lovric
 */
public class TableChangeLegend extends JPanel {
	
	public static final Font LEGEND_FONT = new Font("Lato", Font.BOLD, 14);
	private JTextField textField_1;
	private JTextField txtIncrease;
	private JTextField textField_3;
	private JTextField txtDecrease;
	private JTextField textField_5;
	private JTextField txtNoChange;

	/**
	 * Create the panel.
	 */
	public TableChangeLegend() {
		
		
		super();
		
		//this.setBorder(new LineBorder(new Color(0, 0, 0)));
		this.setBorder(null);
		this.setBackground(LandingGUI.LIGHT_GRAY);
		this.setBounds(0, 0, 138, 77);
		this.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		textField_1 = new JTextField();
		textField_1.setColumns(5);
		textField_1.setBorder(new LineBorder(LandingGUI.DARK_GRAY));
		textField_1.setBackground(LandingGUI.PASTEL_BLUE);
		this.add(textField_1);
		
		txtIncrease = new JTextField();
		txtIncrease.setText("Increase");
		txtIncrease.setColumns(7);
		txtIncrease.setBorder(null);
		txtIncrease.setFont(LEGEND_FONT);
		txtIncrease.setBackground(LandingGUI.LIGHT_GRAY);
		txtIncrease.setForeground(LandingGUI.DARK_GRAY);
		this.add(txtIncrease);
		
		textField_3 = new JTextField();
		textField_3.setColumns(5);
		textField_3.setBorder(new LineBorder(LandingGUI.DARK_GRAY));
		textField_3.setBackground(LandingGUI.PASTEL_YELLOW);
		this.add(textField_3);
		
		txtDecrease = new JTextField();
		txtDecrease.setText("Decrease");
		txtDecrease.setColumns(7);
		txtDecrease.setBorder(null);
		txtDecrease.setFont(LEGEND_FONT);
		txtDecrease.setBackground(LandingGUI.LIGHT_GRAY);
		txtDecrease.setForeground(LandingGUI.DARK_GRAY);
		this.add(txtDecrease);
		
		textField_5 = new JTextField();
		textField_5.setColumns(5);
		textField_5.setBorder(new LineBorder(LandingGUI.DARK_GRAY));
		textField_5.setBackground(Color.WHITE);
		this.add(textField_5);
		
		txtNoChange = new JTextField();
		txtNoChange.setText("No Change");
		txtNoChange.setColumns(7);
		txtNoChange.setBorder(null);
		txtNoChange.setFont(LEGEND_FONT);
		txtNoChange.setBackground(LandingGUI.LIGHT_GRAY);
		txtNoChange.setForeground(LandingGUI.DARK_GRAY);
		this.add(txtNoChange);

	}

}
