package nismod.transport.showcase;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Main GUI for the Show-case Demo
 * @author Milan Lovric
  */
public class GUI {

	private JFrame frame;
	
	//public static final Color DASHBOARD = new Color(149,173,177);
	public static final Color DASHBOARD = new Color(130, 160, 180);
	public static final Color TOOLBAR = new Color(231, 211, 146);
	//(new Color(11,73,84)); //mistral green

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
		frame.setBounds(0, 0, 1920, 1080); //when window is unmaximised
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//frame.setSize(screenSize);
		//frame.setBounds(0,0,screenSize.width, screenSize.height);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); //maximise the screen
		//frame.setAlwaysOnTop(true);
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
		btnRoadDevelopment.setBounds(811, 258, 290, 384);
		btnRoadDevelopment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardRoadDevelopment.main(null);
			}
		});
		
		URL url = getClass().getResource("/images/road.png");
		System.out.println(url.toExternalForm());
		
		StringBuilder html = new StringBuilder();
		html.append("<html><center>");
		html.append("<h2>Intervention 1:</h2>");
		html.append("<h1>Road Expansion</h1>");
		html.append("<img src=\"");
		html.append(url.toExternalForm());
		html.append("\" alt=\"road expansion\" height=\"100\" width=\"100\"> ");
		html.append("<h3>What happens with road expansion?</h3>");
		
		JButton btnRoadExpansion = new JButton(html.toString());
		btnRoadExpansion.setBounds(259, 258, 314, 384);
		btnRoadExpansion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				DashboardRoadExpansion.main(null);
			}
		});
		frame.getContentPane().setLayout(null);
		btnRoadExpansion.setToolTipText("Expands the road link with additional lanes");
		btnRoadExpansion.setForeground(Color.DARK_GRAY);
		btnRoadExpansion.setFont(new Font("Calibri Light", Font.BOLD, 16));
		//btnRoadExpansion.setIcon(icon);
		//btnRoadExpansion.setText("<html>Road Expansion</html>");

		frame.getContentPane().add(btnRoadExpansion);
		btnRoadDevelopment.setToolTipText("Builds new road links between existing intersections");
		btnRoadDevelopment.setForeground(Color.DARK_GRAY);
		btnRoadDevelopment.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnRoadDevelopment.setIcon(icon1);
		frame.getContentPane().add(btnRoadDevelopment);
		
		JButton btnCongestionCharging = new JButton("Congestion Charging");
		btnCongestionCharging.setBounds(1355, 258, 290, 384);
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
	
		frame.pack();
	}
}
