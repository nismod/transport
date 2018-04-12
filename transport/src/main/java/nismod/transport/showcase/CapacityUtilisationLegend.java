package nismod.transport.showcase;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.geotools.brewer.color.ColorBrewer;

public class CapacityUtilisationLegend extends JPanel {

	public static final Font LEGEND_FONT = new Font("Lato", Font.BOLD, 14);

	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;
	private JTextField textField_4;
	private JTextField textField_5;
	private JTextField textField_6;
	private JTextField textField_7;
	private JTextField textField_8;
	private JTextField textField_9;
	private JTextField textField_10;

	/**
	 * Create the panel.
	 */
	public CapacityUtilisationLegend() {
		
		
		super();
		
		this.setBorder(new LineBorder(new Color(0, 0, 0)));
		this.setBackground(Color.WHITE);
		this.setBounds(0, 0, 1000, 28);
		this.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
	    //use a predefined colour palette from the color brewer
	    ColorBrewer brewer = ColorBrewer.instance();
	    //System.out.println(Arrays.toString(brewer.getPaletteNames()));
	    String paletteName = "RdYlGn";
	    Color[] colours = brewer.getPalette(paletteName).getColors(5);
		
		textField = new JTextField();
		textField.setText("Capacity Utilisation:");
		textField.setColumns(13);
		textField.setBorder(null);
		textField.setFont(LEGEND_FONT);
		textField.setForeground(LandingGUI.DARK_GRAY);
		this.add(textField);
		
		textField_1 = new JTextField();
		textField_1.setColumns(5);
		textField_1.setBorder(new LineBorder(new Color(0, 0, 0)));
		textField_1.setBackground(colours[0]);
		this.add(textField_1);
		
		textField_2 = new JTextField();
		textField_2.setText("Very High");
		textField_2.setColumns(7);
		textField_2.setBorder(null);
		textField_2.setFont(LEGEND_FONT);
		textField_2.setForeground(LandingGUI.DARK_GRAY);
		this.add(textField_2);
		
		textField_3 = new JTextField();
		textField_3.setColumns(5);
		textField_3.setBorder(new LineBorder(new Color(0, 0, 0)));
		textField_3.setBackground(colours[1]);
		this.add(textField_3);
		
		textField_4 = new JTextField();
		textField_4.setText("High");
		textField_4.setColumns(7);
		textField_4.setBorder(null);
		textField_4.setFont(LEGEND_FONT);
		textField_4.setForeground(LandingGUI.DARK_GRAY);
		this.add(textField_4);
		
		textField_5 = new JTextField();
		textField_5.setColumns(5);
		textField_5.setBorder(new LineBorder(new Color(0, 0, 0)));
		textField_5.setBackground(colours[2]);
		this.add(textField_5);
		
		textField_6 = new JTextField();
		textField_6.setText("Medium");
		textField_6.setColumns(7);
		textField_6.setBorder(null);
		textField_6.setFont(LEGEND_FONT);
		textField_6.setForeground(LandingGUI.DARK_GRAY);
		this.add(textField_6);
		
		textField_7 = new JTextField();
		textField_7.setColumns(5);
		textField_7.setBorder(new LineBorder(new Color(0, 0, 0)));
		textField_7.setBackground(colours[3]);
		this.add(textField_7);
		
		textField_8 = new JTextField();
		textField_8.setText("Low");
		textField_8.setColumns(7);
		textField_8.setBorder(null);
		textField_8.setFont(LEGEND_FONT);
		textField_8.setForeground(LandingGUI.DARK_GRAY);

		this.add(textField_8);
		
		textField_9 = new JTextField();
		textField_9.setColumns(5);
		textField_9.setBorder(new LineBorder(new Color(0, 0, 0)));
		textField_9.setBackground(colours[4]);
		this.add(textField_9);
		
		textField_10 = new JTextField();
		textField_10.setText("Very Low");
		textField_10.setColumns(7);
		textField_10.setBorder(null);
		textField_10.setFont(LEGEND_FONT);
		textField_10.setForeground(LandingGUI.DARK_GRAY);

		this.add(textField_10);
	
	}
}
