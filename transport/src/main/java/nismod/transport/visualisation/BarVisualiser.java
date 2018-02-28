package nismod.transport.visualisation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * For visualising bar charts using JFreeChart.
 * @author Milan Lovric
  */
public class BarVisualiser extends JFrame {
	
	private final static Logger LOGGER = Logger.getLogger(BarVisualiser.class.getName());
	
	private static DefaultCategoryDataset dataset;
	private static String title;
	private static String paletteName;
	
	public BarVisualiser(DefaultCategoryDataset dataset, String title, String paletteName) throws IOException {

		BarVisualiser.dataset = dataset;
		BarVisualiser.title = title;
		BarVisualiser.paletteName = paletteName;
				
		initUI();
	}

	private void initUI() throws IOException {
		
		JFreeChart chart = createChart(dataset, title);
		
		chart.setRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) );
		chart.getRenderingHints().put(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, Boolean.TRUE);
		chart.setAntiAlias(true);
		
		ChartPanel chartPanel = new ChartPanel(chart);

		chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		chartPanel.setBackground(Color.WHITE);
		this.add(chartPanel);
		
		//chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 367 ) );
	    //setContentPane( chartPanel );
		
	 	CategoryPlot plot = (CategoryPlot) chart.getPlot();
	 	//plot.setBackgroundPaint(Color.black);
	 	//plot.setBackgroundAlpha(0.1f);
	 	plot.setBackgroundPaint(Color.WHITE);
	 	plot.setOutlinePaint(Color.WHITE);
	 	plot.setRangeGridlinePaint(Color.DARK_GRAY);
	 	
	 	chart.getTitle().setPaint(Color.DARK_GRAY);
	 	
	 	BarRenderer barRenderer = (BarRenderer)plot.getRenderer();

		//get brewer palette
		ColorBrewer brewer = ColorBrewer.instance();
		//String paletteName = "BrBG";
		//String paletteName = "Set3";
		int numberOfRowKeys = dataset.getRowKeys().size();
		BrewerPalette palette = brewer.getPalette(paletteName);
		if (palette == null) {
			System.err.println("Invalid brewer palette name. Use one of the following:");
			System.err.println(Arrays.toString(brewer.getPaletteNames()));
		}
		Color[] colors = palette.getColors(numberOfRowKeys);
		for (int i = 0; i < numberOfRowKeys; i++) {
			barRenderer.setSeriesPaint(i, colors[i]);
		}
		 	
	 	this.pack();
		this.setTitle("NISMOD v2");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//Image myImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("NISMOD-LP.jpg"));
		ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		this.setIconImage(icon.getImage());

		//this.setSize(600, 400);
		//ChartUtils.saveChartAsPNG(new File("pie_chart.png"), chart, 600, 400);
	}
	
	public void saveToPNG(String fileName) throws IOException {
		
		BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();

		//this.getContentPane().paint(g2d);
		//this.printComponents(g2d);
		this.print(g2d);
		//this.printAll(g2d);
		//this.paint(g2d);
			
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		ImageIO.write(img, "png", new File(fileName));
			
		g2d.dispose();
	}

	private JFreeChart createChart(DefaultCategoryDataset dataset, String title) {

		JFreeChart barChart;
		
		barChart = ChartFactory.createBarChart(title, "", "", dataset, PlotOrientation.VERTICAL, true, true, false);
		return barChart;
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			BarVisualiser pc;
			try {
				pc = new BarVisualiser(dataset, title, paletteName);
				pc.setVisible(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
	}
}

