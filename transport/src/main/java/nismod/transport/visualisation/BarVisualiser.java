package nismod.transport.visualisation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import nismod.transport.showcase.LandingGUI;

/**
 * For visualising bar charts using JFreeChart.
 * @author Milan Lovric
  */
public class BarVisualiser extends JFrame {
	
	private final static Logger LOGGER = LogManager.getLogger();
	
	private static DefaultCategoryDataset dataset;
	private static String title;
	private static String paletteName;
	private static boolean invertColours;
	
	public BarVisualiser(DefaultCategoryDataset dataset, String title, String paletteName, boolean invertColours) throws IOException {

		BarVisualiser.dataset = dataset;
		BarVisualiser.title = title;
		BarVisualiser.paletteName = paletteName;
		BarVisualiser.invertColours = invertColours;
				
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
	 	
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);
	 	
		Font font3 = new Font("Tahoma", Font.BOLD, 14); 
	 	plot.getDomainAxisForDataset(0).setLabelFont(font3);
	 	plot.getDomainAxis().setLabelFont(font3);
	 	plot.getDomainAxis(0).setLabelFont(font3);
	 	plot.getRangeAxis().setLabelFont(font3);
	 	
	 	//setting the legend font (NO, B1, B3)
	 	chart.getLegend().setItemFont(font3);
	 	
	 	//setting the below bars font (OX <-> MK)
	 	plot.getDomainAxis().setTickLabelFont(font3);

	 	//setting the range axis font
	 	plot.getRangeAxis().setTickLabelFont(font3);
	 	
	 	BarRenderer barRenderer = (BarRenderer)plot.getRenderer();
		barRenderer.setBarPainter(new StandardBarPainter()); //to remove gradient bar painter
		barRenderer.setDrawBarOutline(false);
		barRenderer.setShadowVisible(false);
		//barRenderer.setMaximumBarWidth(0.30);

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
			if (invertColours)
				barRenderer.setSeriesPaint(i, colors[numberOfRowKeys-1-i]);
			else
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
	
	public void saveToPNG(String fileName) throws IOException, ImageWriteException {
		
		BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		
		//temporarily set undecorated to remove the black frame in the written file
		this.dispose(); 
		this.setUndecorated(true); 
		this.setVisible(true);

		//this.getContentPane().paint(g2d);
		//this.printComponents(g2d);
		this.print(g2d);
		//this.printAll(g2d);
		//this.paint(g2d);
			
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Imaging.writeImage(img, new File(fileName), ImageFormats.PNG, null);
				
		g2d.dispose();
		
		this.dispose(); 
		this.setUndecorated(false); 
		this.setVisible(true);
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
				pc = new BarVisualiser(dataset, title, paletteName, invertColours);
				pc.setVisible(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error(e);
			}

		});
	}
}

