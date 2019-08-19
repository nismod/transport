package nismod.transport.visualisation;

import java.awt.Color;
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
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

/**
 * For visualising pie charts using JFreeChart.
 * @author Milan Lovric
  */
public class PieChartVisualiser extends JFrame {
	
	private final static Logger LOGGER = LogManager.getLogger(PieChartVisualiser.class);
	
	private static DefaultPieDataset dataset;
	private static String title;
	private static String paletteName;
	private static boolean threeD;

	public PieChartVisualiser(DefaultPieDataset dataset, String title, String paletteName, boolean threeD) throws IOException {

		PieChartVisualiser.dataset = dataset;
		PieChartVisualiser.title = title;
		PieChartVisualiser.paletteName = paletteName;
		PieChartVisualiser.threeD = threeD;
		
		initUI();
	}

	private void initUI() throws IOException {
		
		//DefaultPieDataset dataset = createDataset();
		JFreeChart chart = createChart(dataset, title);
		
		chart.setRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) );
		chart.getRenderingHints().put(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, Boolean.TRUE);
		chart.setAntiAlias(true);
		
		//label with percentages
		PieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator("{0} = {2}");

		//get brewer palette
		ColorBrewer brewer = ColorBrewer.instance();
		//String paletteName = "BrBG";
		//String paletteName = "Set3";
		int numberOfPieElements = dataset.getKeys().size();
		BrewerPalette palette = brewer.getPalette(paletteName);
		if (palette == null) {
			System.err.println("Invalid brewer palette name. Use one of the following:");
			System.err.println(Arrays.toString(brewer.getPaletteNames()));
		}
		Color[] colors = palette.getColors(numberOfPieElements);
		
		if (threeD) {
			PiePlot3D plot = (PiePlot3D) chart.getPlot(); 
			plot = (PiePlot3D) plot;
			plot.setStartAngle(270);             
			plot.setForegroundAlpha(1.00f);
			plot.setDarkerSides(true);
			plot.setIgnoreZeroValues(true);
			plot.setInteriorGap(0.02);
			plot.setBackgroundPaint(Color.WHITE);
			plot.setLabelGenerator(labelGenerator);
			//use brewer palette for each section
			for (int i = 0; i < numberOfPieElements; i++) {
				plot.setSectionPaint(dataset.getKey(i), colors[i]);
			}
		} else {
	    	PiePlot plot = (PiePlot) chart.getPlot();
	    	plot.setStartAngle(180); 
	    	plot.setIgnoreZeroValues(true);
			plot.setInteriorGap(0.03);
			plot.setBackgroundPaint(Color.WHITE);
			plot.setLabelGenerator(labelGenerator);
			//use brewer palette for each section
			for (int i = 0; i < numberOfPieElements; i++) {
				plot.setSectionPaint(dataset.getKey(i), colors[i]);
			}
		}
		
		ChartPanel chartPanel = new ChartPanel(chart);

		chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		chartPanel.setBackground(Color.white);
		this.add(chartPanel);

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
		Imaging.writeImage(img, new File(fileName), ImageFormats.PNG , null);
			
		g2d.dispose();
		
		this.dispose(); 
		this.setUndecorated(false); 
		this.setVisible(true);
	}

	private JFreeChart createChart(DefaultPieDataset dataset, String title) {

		JFreeChart pieChart;
		
		if (threeD) pieChart = ChartFactory.createPieChart3D(title,	dataset, true, true, false);
		else pieChart = ChartFactory.createPieChart(title, dataset, true, true, false);

		return pieChart;
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			PieChartVisualiser pc;
			try {
				pc = new PieChartVisualiser(dataset, title, paletteName, threeD);
				pc.setVisible(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error(e);
			}

		});
	}
}

