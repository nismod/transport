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
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import javax.swing.SwingConstants;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Main GUI for the Show-case Demo
 * @author Milan Lovric
  */
public class GUItest {

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
					GUItest window = new GUItest();
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
	public GUItest() {
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
		
			
		URL url = getClass().getResource("/images/road.png");
		System.out.println(url.toExternalForm());
		
		StringBuilder html = new StringBuilder();
		html.append("<html><center>");
		html.append("<h2>Intervention 1:</h2>");
		html.append("<h1>Road Expansion</h1><br>");
		html.append("<img src=\"");
		html.append(url.toExternalForm());
		html.append("\" alt=\"road expansion\" height=\"100\" width=\"100\"> ");
		html.append("<br><br><h3>What happens when<br>we increase road capacity<br>by adding lanes?</h3>");
		
		JButton btnRoadExpansion = new JButton(html.toString());
		btnRoadExpansion.setBounds(258, 341, 300, 400);
		btnRoadExpansion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				DashboardRoadExpansion.main(null);
			}
		});
		frame.getContentPane().setLayout(null);
		btnRoadExpansion.setToolTipText("Expands the road link with additional lanes");
		btnRoadExpansion.setForeground(Color.DARK_GRAY);
		//btnRoadExpansion.setFont(new Font("Calibri Light", Font.BOLD, 32));
		btnRoadExpansion.setFont(new Font("Tahoma", Font.PLAIN, 20));
		//btnRoadExpansion.setIcon(icon);
		//btnRoadExpansion.setText("<html>Road Expansion<br>Amazing</html>");
		//btnRoadExpansion.focu
		frame.getContentPane().add(btnRoadExpansion);
		
		// We cast the frame in order to use the method setBorder()
		JPanel contentPane = (JPanel) frame.getContentPane();
		contentPane.setFocusable(true);
		contentPane.requestFocusInWindow();
		
		StringBuilder contentBuilder = new StringBuilder();
		try {
		    //BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\ml2e16\\git\\nismod\\models\\transport\\transport\\src\\test\\resources\\html\\Intervention 1.htm"));
			BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\ml2e16\\git\\nismod\\models\\transport\\transport\\src\\test\\resources\\html\\intervention2.html"));
		    String str;
		    while ((str = in.readLine()) != null) {
		        contentBuilder.append(str);
		    }
		    in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String content = contentBuilder.toString();
		System.out.println("bablabladblab");
		
		
		StringBuilder html2 = new StringBuilder();
		html2.append("<html><center>");
		html2.append("<h2>Intervention 1:</h2>");
		html2.append("<h1>Road Expansion</h1><br>");
		html2.append("<h3>What happens when<br>we increase road capacity<br>by adding lanes?</h3>");
		
//		JEditorPane jep = new JEditorPane("text/html", content);
//		jep.setBounds(110, 721, 498, 269);
//		frame.getContentPane().add(jep);
			
		JButton btnNewButton_1 = new JButton(html2.toString());
		btnNewButton_1.setForeground(Color.DARK_GRAY);
		btnNewButton_1.setBounds(732, 341, 300, 400);
		frame.getContentPane().add(btnNewButton_1);
		// Place text above icon
		btnNewButton_1.setVerticalTextPosition(SwingConstants.TOP);
		btnNewButton_1.setHorizontalTextPosition(SwingConstants.CENTER);
		icon = new ImageIcon("./src/test/resources/images/road.png");
		img = icon.getImage() ;  
		newimg = img.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon = new ImageIcon(newimg);
		btnNewButton_1.setIcon(icon);
	
		StringBuilder html3 = new StringBuilder();
		html3.append("<html><center>");
		html3.append("<h2>Intervention 1:</h2>");
		html3.append("<h1>Road Expansion</h1><br>");
		
		//JButton btnNewButton = new JButton(content, icon);
		JButton btnNewButton = new JButton(html3.toString());
		btnNewButton.setToolTipText("<html><center><h3>What happens when<br>we increase road capacity<br>by adding lanes?</h3></html>");
		btnNewButton.setForeground(Color.DARK_GRAY);
		btnNewButton.setBounds(1216, 341, 300, 400);
		frame.getContentPane().add(btnNewButton);
		// Place text above icon
		btnNewButton.setVerticalTextPosition(SwingConstants.TOP);
		btnNewButton.setHorizontalTextPosition(SwingConstants.CENTER);
		icon = new ImageIcon("./src/test/resources/images/road.png");
		img = icon.getImage() ;  
		newimg = img.getScaledInstance(150, 150,  java.awt.Image.SCALE_SMOOTH ) ;  
		icon = new ImageIcon(newimg);
		btnNewButton.setIcon(icon);

	
		
		
		
		
		
		JLabel lblWhatIsThe = new JLabel("What is the impact of traffic policy interventions in South East England?");
		lblWhatIsThe.setHorizontalAlignment(SwingConstants.CENTER);
		lblWhatIsThe.setForeground(Color.DARK_GRAY);
		lblWhatIsThe.setFont(new Font("Tahoma", Font.PLAIN, 36));
		lblWhatIsThe.setBounds(258, 50, 1286, 66);
		frame.getContentPane().add(lblWhatIsThe);
		
		JLabel lblTheMistralRoad = new JLabel("The MISTRAL road traffic model predicts passenger and freight vehicle flows on major roads in Great Britain.");
		lblTheMistralRoad.setHorizontalAlignment(SwingConstants.CENTER);
		lblTheMistralRoad.setForeground(Color.DARK_GRAY);
		lblTheMistralRoad.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblTheMistralRoad.setBounds(259, 127, 1236, 66);
		frame.getContentPane().add(lblTheMistralRoad);
		
		JLabel lblClickHere = new JLabel("Click to explore how three policy interventions would influence road capacity utilisation, vehicle demand and travel times.");
		lblClickHere.setHorizontalAlignment(SwingConstants.CENTER);
		lblClickHere.setForeground(Color.DARK_GRAY);
		lblClickHere.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblClickHere.setBounds(259, 179, 1236, 66);
		frame.getContentPane().add(lblClickHere);
		
		JLabel lblItrcMistralTransport = new JLabel("ITRC MISTRAL Transport Model | To learn more, please contact Milan Lovric, University of Southampton (M.Lovric@soton.ac.uk)");
		lblItrcMistralTransport.setHorizontalAlignment(SwingConstants.CENTER);
		lblItrcMistralTransport.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblItrcMistralTransport.setForeground(Color.DARK_GRAY);
		lblItrcMistralTransport.setBounds(254, 912, 1262, 66);
		frame.getContentPane().add(lblItrcMistralTransport);
		
		JLabel lblNewLabel = new JLabel("<html><center><h3>What happens when<br>we increase road capacity<br>by adding lanes?</h3></html>");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setForeground(Color.DARK_GRAY);
		lblNewLabel.setBounds(1216, 752, 300, 99);
		frame.getContentPane().add(lblNewLabel);
				
		
		frame.pack();
		frame.setVisible(true);
		
		
		
//		<html><center>
//		<img src="image001.png" alt="road expansion" height="242" width="242"> 
//		<body>
//		<h2>Intervention 1:</h2>
//		<h1>Road Expansion</h1>
//		<img src="file://image001.png" alt="road expansion" height="42" width="42"> 
//		<p style="font-size: 1.0em; text-align:center">What happens when we<br> increase road capacity<br> by adding lanes?</p>
//		<p>&nbsp;</p>
//		</body>
//		<img src="image001.png" alt="road expansion" height="242" width="242"> 
//		</html>
//		
				
//		try {
//		URL u=new URL("http://www.mysite.com");
//		JEditorPane jep = new JEditorPane(u);
//		jep.setEditable(false);
//		jep.setBounds(110, 721, 278, 197);
//		frame.getContentPane().add(jep);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	
	}
}
