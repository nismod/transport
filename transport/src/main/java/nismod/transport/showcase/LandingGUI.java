package nismod.transport.showcase;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/**
 * Main GUI for the Show-case Demo
 * @author Milan Lovric
  */
public class LandingGUI {

	private JFrame frame;
	
	//counter for the output shapefiles (after intervenetion)
	public static int counter = 1;
	
	public static final Color DASHBOARD = new Color(238, 238, 238);
	public static final Color TOOLBAR = new Color(231, 211, 146);
	
	public static final Color DARK_GRAY = Color.decode("#41444A");
	public static final Color MID_GRAY = Color.decode("#ADADAD");
	public static final Color LIGHT_GRAY = Color.decode("#EEEEEE");
	public static final Color PASTEL_GREEN = Color.decode("#7ED6BC");
	public static final Color PASTEL_YELLOW = Color.decode("#F3D24F");
	public static final Color PASTEL_BLUE = Color.decode("#91CCF4");
	
	public static final int MAIN_TITLE_FONT_SIZE = 42;
	public static final int SUBTITLE_FONT_SIZE = 30;
	public static final int CREDITS_FONT_SIZE = 24;
	
	public static final int SCREEN_WIDTH = 1920;
	public static final int SCREEN_HEIGHT = 1080;
	public static final int BUTTON_WIDTH = 420;
	public static final int BUTTON_HEIGHT = 320;
	public static final int BUTTON_SPACE = 50; //the space between buttons
	public static final int BUTTON_Y = 340; //distance from the top window border to top of buttons
	public static final int BUTTON_X = (SCREEN_WIDTH - 3 * BUTTON_WIDTH - 2 * BUTTON_SPACE) / 2; //distance from left window border to first button
	
	public static final int LABEL_WIDTH = 1360;
	public static final int LABEL_HEIGHT = 66;
	public static final int LABEL_X = (SCREEN_WIDTH - LABEL_WIDTH) / 2;
	public static final int LABEL1_Y = 100; //title
	public static final int LABEL2_Y = 190; //subtitle
	public static final int LABEL3_Y = 900; //credits
		
	public static final int ICON1_WIDTH = 175;
	public static final int ICON1_HEIGHT = 175;
	public static final int ICON2_WIDTH = 235;
	public static final int ICON2_HEIGHT = 175;
	public static final int ICON3_WIDTH = 200;
	public static final int ICON3_HEIGHT = 175;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LandingGUI window = new LandingGUI();
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
	public LandingGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); //when window is unmaximised
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//frame.setSize(screenSize);
		//frame.setBounds(0,0,screenSize.width, screenSize.height);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); //maximise the screen
		//frame.setAlwaysOnTop(true);
		frame.setTitle("NISMOD v2 Showcase Demo");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage("./src/test/resources/images/NISMOD-LP.jpg"));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		frame.setBackground(LIGHT_GRAY);
		
		JPanel contentPane = (JPanel) frame.getContentPane();
		contentPane.setFocusable(true);
		contentPane.requestFocusInWindow();
		
		File img = new File("./src/test/resources/images/roadIcon.png");
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(img);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//BufferedImage subImage = bufferedImage.getSubimage(0, 10, bufferedImage.getWidth(), bufferedImage.getHeight() - 20); //trimming
		Image newimg = bufferedImage.getScaledInstance(ICON1_WIDTH, ICON1_HEIGHT, java.awt.Image.SCALE_SMOOTH); //scaling  
		ImageIcon icon1 = new ImageIcon(newimg);

		File img2 = new File("./src/test/resources/images/roadworksIcon.png");
		BufferedImage bufferedImage2 = null;
		try {
			bufferedImage2 = ImageIO.read(img2);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedImage subImage2 = bufferedImage2.getSubimage(0, 50, bufferedImage.getWidth(), bufferedImage.getHeight() - 110); //trimming
		Image newimg2 = subImage2.getScaledInstance(ICON2_WIDTH, ICON2_HEIGHT, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon icon2 = new ImageIcon(newimg2);
		
		File img3 = new File("./src/test/resources/images/tollGateIcon.png");
		BufferedImage bufferedImage3 = null;
		try {
			bufferedImage3 = ImageIO.read(img3);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedImage subImage3 = bufferedImage3.getSubimage(0, 30, bufferedImage.getWidth(), bufferedImage.getHeight() - 60); //trimming
		Image newimg3 = subImage3.getScaledInstance(ICON3_WIDTH, ICON3_HEIGHT, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon icon3 = new ImageIcon(newimg3);
		
		JLabel lblTitle = new JLabel("What is the impact of traffic policy interventions in South East England?");
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setVerticalAlignment(SwingConstants.TOP);
		lblTitle.setForeground(Color.BLACK);
		lblTitle.setFont(new Font("Lato", Font.BOLD, MAIN_TITLE_FONT_SIZE));
		lblTitle.setBounds(LABEL_X, LABEL1_Y, LABEL_WIDTH, LABEL_HEIGHT);
		frame.getContentPane().add(lblTitle);
		
		JLabel lblSubTitle = new JLabel("<html><center>Click to explore how three policy interventions would inﬂuence road capacity utilisation,<br>vehicle demand and travel times on major roads in Great Britain.</center></html>");
		lblSubTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblSubTitle.setVerticalAlignment(SwingConstants.TOP);
		lblSubTitle.setForeground(Color.DARK_GRAY);
		//lblSubTitle.setForeground(new Color(11,73,84));
		lblSubTitle.setFont(new Font("Lato", Font.PLAIN, SUBTITLE_FONT_SIZE));
		lblSubTitle.setBounds(LABEL_X, LABEL2_Y, LABEL_WIDTH, LABEL_HEIGHT * 2);
		frame.getContentPane().add(lblSubTitle);

		StringBuilder html1 = new StringBuilder();
		html1.append("<html><center>");
		html1.append("<font size=+2><b>Intervention 1:</b></font><br>");
		html1.append("<font size=+5><b>Road Expansion</b></font></html>");
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnRoadExpansion = new JButton(html1.toString());
		btnRoadExpansion.setToolTipText("<html><center><h3>What happens when we<br>increase road capacity<br>by adding lanes?</h3></html>");
		btnRoadExpansion.setForeground(DARK_GRAY);
		btnRoadExpansion.setFont(new Font("Lato", Font.PLAIN, 24));
		btnRoadExpansion.setBounds(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
		// Place text above icon
		btnRoadExpansion.setVerticalTextPosition(SwingConstants.TOP);
		btnRoadExpansion.setHorizontalTextPosition(SwingConstants.CENTER);
		btnRoadExpansion.setIcon(icon1);
		btnRoadExpansion.setBackground(PASTEL_GREEN);
		btnRoadExpansion.setBorderPainted(false);
		btnRoadExpansion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardRoadExpansion.main(null);
			}
		});
		btnRoadExpansion.setFocusable(false);
		frame.getContentPane().add(btnRoadExpansion);
		
		JLabel lblRoadExpansion = new JLabel("<html><center>What happens when we<br>increase road capacity<br>by adding lanes?</html>");
		lblRoadExpansion.setHorizontalAlignment(SwingConstants.CENTER);
		lblRoadExpansion.setForeground(DARK_GRAY);
		lblRoadExpansion.setFont(new Font("Lato", Font.PLAIN, SUBTITLE_FONT_SIZE));
		lblRoadExpansion.setBounds(BUTTON_X, BUTTON_Y + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT / 2);
		frame.getContentPane().add(lblRoadExpansion);

		StringBuilder html2 = new StringBuilder();
		html2.append("<html><center>");
		html2.append("<font size=+2><b>Intervention 2:</b></font><br>");
		html2.append("<font size=+5><b>Road Development</b></font></html>");
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnRoadDevelopment = new JButton(html2.toString());
		btnRoadDevelopment.setToolTipText("<html><center><h3>What happens when we<br>increase road capacity<br>by building new roads?</h3></html>");
		btnRoadDevelopment.setForeground(Color.DARK_GRAY);
		btnRoadDevelopment.setFont(new Font("Lato", Font.PLAIN, 20));
		btnRoadDevelopment.setBounds(BUTTON_X + BUTTON_WIDTH + BUTTON_SPACE, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
		// Place text above icon
		btnRoadDevelopment.setVerticalTextPosition(SwingConstants.TOP);
		btnRoadDevelopment.setHorizontalTextPosition(SwingConstants.CENTER);
		btnRoadDevelopment.setIcon(icon2);
		btnRoadDevelopment.setBackground(PASTEL_YELLOW);
		btnRoadDevelopment.setBorderPainted(false);
		btnRoadDevelopment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardRoadDevelopment.main(null);
			}
		});
		btnRoadDevelopment.setFocusable(false);
		frame.getContentPane().add(btnRoadDevelopment);

		JLabel lblRoadDevelopment = new JLabel("<html><center>What happens when we<br>increase road capacity<br>by building new roads?</html>");
		lblRoadDevelopment.setHorizontalAlignment(SwingConstants.CENTER);
		lblRoadDevelopment.setForeground(DARK_GRAY);
		lblRoadDevelopment.setFont(new Font("Lato", Font.PLAIN, SUBTITLE_FONT_SIZE));
		lblRoadDevelopment.setBounds(BUTTON_X + BUTTON_WIDTH + BUTTON_SPACE, BUTTON_Y + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT / 2);
		frame.getContentPane().add(lblRoadDevelopment);

		StringBuilder html3 = new StringBuilder();
		html3.append("<html><center>");
		html3.append("<font size=+2><b>Intervention 3:</b></font><br>");
		html3.append("<font size=+5><b>Congestion Charging</b></font></html>"); //<br> for vertical space
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnCongestionCharging = new JButton(html3.toString());
		btnCongestionCharging.setToolTipText("<html><center><h3>What happens when we<br>implement a congestion<br>charging zone?</h3></html>");
		btnCongestionCharging.setForeground(Color.DARK_GRAY);
		btnCongestionCharging.setFont(new Font("Lato", Font.PLAIN, 20));
		btnCongestionCharging.setBounds(BUTTON_X + 2 * (BUTTON_WIDTH + BUTTON_SPACE), BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
		// Place text above icon
		btnCongestionCharging.setVerticalTextPosition(SwingConstants.TOP);
		btnCongestionCharging.setHorizontalTextPosition(SwingConstants.CENTER);
		btnCongestionCharging.setIcon(icon3);
		btnCongestionCharging.setBackground(PASTEL_BLUE);
		btnCongestionCharging.setBorderPainted(false);
		btnCongestionCharging.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DashboardCongestionCharging.main(null);
			}
		});
		btnCongestionCharging.setFocusable(false);
		frame.getContentPane().add(btnCongestionCharging);
		
		JLabel lblCongestionCharging = new JLabel("<html><center>What happens when we<br>implement a congestion<br>charging zone?</html>");
		lblCongestionCharging.setHorizontalAlignment(SwingConstants.CENTER);
		lblCongestionCharging.setForeground(DARK_GRAY);
		lblCongestionCharging.setFont(new Font("Lato", Font.PLAIN, SUBTITLE_FONT_SIZE));
		lblCongestionCharging.setBounds(BUTTON_X + 2 * (BUTTON_WIDTH + BUTTON_SPACE), BUTTON_Y + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT / 2);
		frame.getContentPane().add(lblCongestionCharging);
		
		JLabel lblItrcMistralTransport = new JLabel("ITRC MISTRAL Transport Model | To learn more, please contact Milan Lovric, University of Southampton (M.Lovric@soton.ac.uk)");
		lblItrcMistralTransport.setHorizontalAlignment(SwingConstants.CENTER);
		lblItrcMistralTransport.setFont(new Font("Lato", Font.PLAIN, CREDITS_FONT_SIZE));
		lblItrcMistralTransport.setForeground(MID_GRAY);
		lblItrcMistralTransport.setBounds(LABEL_X, LABEL3_Y, LABEL_WIDTH, LABEL_HEIGHT);
		frame.getContentPane().add(lblItrcMistralTransport);

		frame.pack();
	}
}
