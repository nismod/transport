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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Main GUI for the Show-case Demo
 * @author Milan Lovric
  */
public class GUI {

	private JFrame frame;
	
	//public static final Color DASHBOARD = new Color(149,173,177);
	public static final Color DASHBOARD = new Color(238, 238, 238);
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
		frame.getContentPane().setLayout(null);
		
		JPanel contentPane = (JPanel) frame.getContentPane();
		contentPane.setFocusable(true);
		contentPane.requestFocusInWindow();
		
		ImageIcon icon1 = new ImageIcon("./src/test/resources/images/road.png");
		Image img = icon1.getImage() ;  
		Image newimg = img.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon1 = new ImageIcon(newimg);
		ImageIcon icon2 = new ImageIcon("./src/test/resources/images/roadworks.png");
		Image img2 = icon2.getImage() ;  
		Image newimg2 = img2.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon2 = new ImageIcon(newimg2);
		ImageIcon icon3 = new ImageIcon("./src/test/resources/images/moneybag.png");
		Image img3 = icon3.getImage() ;  
		Image newimg3 = img3.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon3 = new ImageIcon(newimg3);

		
		JLabel lblTitle = new JLabel("NISMOD v2 Transport Model");
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setForeground(Color.DARK_GRAY);
		lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 36));
		lblTitle.setBounds(258, 100, 1286, 66);
		frame.getContentPane().add(lblTitle);
		
		JLabel lblShowcase = new JLabel("Showcase Demo");
		lblShowcase.setHorizontalAlignment(SwingConstants.CENTER);
		lblShowcase.setForeground(new Color(11,73,84));
		lblShowcase.setFont(new Font("Tahoma", Font.ITALIC, 32));
		lblShowcase.setBounds(250, 175, 1286, 66);
		frame.getContentPane().add(lblShowcase);

		StringBuilder html1 = new StringBuilder();
		html1.append("<html><center>");
		html1.append("<h2>Intervention 1:</h2>");
		html1.append("<h1>Road Expansion</h1><br><br>");
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnRoadExpansion = new JButton(html1.toString());
		btnRoadExpansion.setToolTipText("<html><center><h3>What happens when<br>we increase road capacity<br>by adding lanes?</h3></html>");
		btnRoadExpansion.setForeground(Color.DARK_GRAY);
		btnRoadExpansion.setFont(new Font("Tahoma", Font.PLAIN, 20));
		btnRoadExpansion.setBounds(300, 341, 300, 400);
		// Place text above icon
		btnRoadExpansion.setVerticalTextPosition(SwingConstants.TOP);
		btnRoadExpansion.setHorizontalTextPosition(SwingConstants.CENTER);
		btnRoadExpansion.setIcon(icon1);
		btnRoadExpansion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardRoadExpansion.main(null);
			}
		});
		//btnRoadExpansion.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnRoadExpansion.setFocusable(false);
		frame.getContentPane().add(btnRoadExpansion);


		StringBuilder html2 = new StringBuilder();
		html2.append("<html><center>");
		html2.append("<h2>Intervention 2:</h2>");
		html2.append("<h1>Road Development</h1><br><br>");
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnRoadDeveloment = new JButton(html2.toString());
		btnRoadDeveloment.setToolTipText("<html><center><h3>What happens when<br>we increase road capacity<br>by building new roads?</h3></html>");
		btnRoadDeveloment.setForeground(Color.DARK_GRAY);
		btnRoadDeveloment.setFont(new Font("Tahoma", Font.PLAIN, 20));
		btnRoadDeveloment.setBounds(800, 341, 300, 400);
		// Place text above icon
		btnRoadDeveloment.setVerticalTextPosition(SwingConstants.TOP);
		btnRoadDeveloment.setHorizontalTextPosition(SwingConstants.CENTER);
		btnRoadDeveloment.setIcon(icon2);
		btnRoadDeveloment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardRoadDevelopment.main(null);
			}
		});
		//btnRoadDeveloment.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnRoadDeveloment.setFocusable(false);
		frame.getContentPane().add(btnRoadDeveloment);


		StringBuilder html3 = new StringBuilder();
		html3.append("<html><center>");
		html3.append("<h2>Intervention 3:</h2>");
		html3.append("<h1>Congestion Charging</h1><br><br>");
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnCongestionCharging = new JButton(html3.toString());
		btnCongestionCharging.setToolTipText("<html><center><h3>What happens when<br>we implement congestion<br>charging zone?</h3></html>");
		btnCongestionCharging.setForeground(Color.DARK_GRAY);
		btnCongestionCharging.setFont(new Font("Tahoma", Font.PLAIN, 20));
		btnCongestionCharging.setBounds(1300, 341, 300, 400);
		// Place text above icon
		btnCongestionCharging.setVerticalTextPosition(SwingConstants.TOP);
		btnCongestionCharging.setHorizontalTextPosition(SwingConstants.CENTER);
		btnCongestionCharging.setIcon(icon3);
		btnCongestionCharging.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardCongestionCharging.main(null);
			}
		});
		//btnCongestionCharging.setFont(new Font("Calibri Light", Font.BOLD, 16));
		btnCongestionCharging.setFocusable(false);
		frame.getContentPane().add(btnCongestionCharging);

		frame.pack();
	}
}
