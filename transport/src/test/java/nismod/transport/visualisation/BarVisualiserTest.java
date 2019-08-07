package nismod.transport.visualisation;

import java.io.IOException;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

/**
 * @author Milan Lovric
 *
 */
public class BarVisualiserTest {
	
	public static void main( String[] args ) throws IOException	{

		
		DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
		
		barDataset.addValue(70050.0, "No intervention", "Number of Trips");
		barDataset.addValue(70100.0, "Road expansion", "Number of Trips");
		barDataset.addValue(70150.0, "Road development", "Number of Trips");

		BarVisualiser bar = new BarVisualiser(barDataset, "Impact of New Infrastructure on Demand", "Set2", false);
		bar.setSize(600, 400);
		bar.setVisible(true);
		//line.saveToPNG("BarVisualiserTest.png");
			
		DefaultCategoryDataset barDataset2 = new DefaultCategoryDataset();
		
		barDataset2.addValue(100.0, "No intervention", "Number of Trips");
		barDataset2.addValue(80.0, "Congestion charging", "Number of Trips");
		barDataset2.addValue(60.0, "No intervention", "Number of Trips Through the Zone");
		barDataset2.addValue(50.0, "Congestion charging", "Number of Trips Through the Zone");
		barDataset2.addValue(45.0, "No intervention", "Number of Trips Outside the Zone");
		barDataset2.addValue(40.0, "Congestion charging", "Number of Trips Outside the Zone");
		
		BarVisualiser bar2 = new BarVisualiser(barDataset2, "Impact of Congestion Charging on Demand", "Set2", true);
		bar2.setSize(600, 400);
		bar2.setVisible(true);
		//line.saveToPNG("BarVisualiserTest2.png");
		
	}
}