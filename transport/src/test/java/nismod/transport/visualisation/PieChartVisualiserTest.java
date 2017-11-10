package nismod.transport.visualisation;

import java.io.IOException;

import org.jfree.data.general.DefaultPieDataset;

/**
 * @author Milan Lovric
 *
 */
public class PieChartVisualiserTest {
	
	public static void main( String[] args ) throws IOException	{
		
		DefaultPieDataset pieDataset = new DefaultPieDataset();
		
		pieDataset.setValue("A", 0.4);
		pieDataset.setValue("B", 0.2);
		pieDataset.setValue("C", 0.05);
		pieDataset.setValue("D", 0.3);
		pieDataset.setValue("E", 0.05);
				
		PieChartVisualiser pie = new PieChartVisualiser(pieDataset, "This is a pie chart", "Set3", false);
		pie.setVisible(true);
		pie.saveToPNG("PieChartVisualiserTest.png");
		
		PieChartVisualiser pie2 = new PieChartVisualiser(pieDataset, "This is a 3D pie chart", "BrBG", true);
		pie2.setSize(600, 400);
		pie2.setVisible(true);
		pie2.saveToPNG("PieChartVisualiserTest3D.png");
	}
}