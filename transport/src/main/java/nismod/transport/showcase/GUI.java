package nismod.transport.showcase;

import java.awt.EventQueue;
import java.awt.Toolkit;

import javax.swing.JFrame;

import org.geotools.swing.JMapFrame;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Window.Type;
import java.awt.FlowLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Main GUI for the Show-case Demo
 * @author Milan Lovric
  */
public class GUI {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 1024, 768);
		//frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setTitle("NISMOD v2 Showcase Demo");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage("./src/test/resources/images/NISMOD-LP.jpg"));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ImageIcon icon = new ImageIcon("./src/test/resources/images/road.png");
		Image img = icon.getImage() ;  
		Image newimg = img.getScaledInstance(100, 40,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon = new ImageIcon(newimg);
		ImageIcon icon1 = new ImageIcon("./src/test/resources/images/roadworks.png");
		Image img1 = icon1.getImage() ;  
		Image newimg1 = img1.getScaledInstance(100, 40,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon1 = new ImageIcon(newimg1);
		ImageIcon icon2 = new ImageIcon("./src/test/resources/images/moneybag.png");
		Image img2 = icon2.getImage() ;  
		Image newimg2 = img2.getScaledInstance(100, 40,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon2 = new ImageIcon(newimg2);
		
		JButton btnRoadDevelopment = new JButton("Road Development");
		btnRoadDevelopment.setBounds(359, 258, 290, 180);
		btnRoadDevelopment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardRoadDevelopment.main(null);
			}
		});
		
		JButton btnRoadExpansion = new JButton("Road Expansion");
		btnRoadExpansion.setBounds(31, 258, 290, 180);
		btnRoadExpansion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				DashboardRoadExpansion.main(null);
			}
		});
		frame.getContentPane().setLayout(null);
		btnRoadExpansion.setToolTipText("Expands the road link with additional lanes");
		btnRoadExpansion.setForeground(Color.DARK_GRAY);
		btnRoadExpansion.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnRoadExpansion.setIcon(icon);
		frame.getContentPane().add(btnRoadExpansion);
		btnRoadDevelopment.setToolTipText("Builds new road links between existing intersections");
		btnRoadDevelopment.setForeground(Color.DARK_GRAY);
		btnRoadDevelopment.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnRoadDevelopment.setIcon(icon1);
		frame.getContentPane().add(btnRoadDevelopment);
		
		JButton btnCongestionCharging = new JButton("Congestion Charging");
		btnCongestionCharging.setBounds(683, 258, 290, 180);
		btnCongestionCharging.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardCongestionCharging.main(null);
			}
		});
		btnCongestionCharging.setToolTipText("Expands the road link with additional lanes");
		btnCongestionCharging.setForeground(Color.DARK_GRAY);
		btnCongestionCharging.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnCongestionCharging.setToolTipText("Implements a congestion charging zone");
		btnCongestionCharging.setIcon(icon2);
		frame.getContentPane().add(btnCongestionCharging);
	}
}
