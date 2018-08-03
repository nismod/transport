package nismod.transport.visualisation;

import java.io.IOException;

import org.apache.sanselan.ImageWriteException;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

/**
 * @author Milan Lovric
 *
 */
public class LineVisualiserTest {
	
	public static void main( String[] args ) throws IOException, ImageWriteException	{
		
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		
		lineDataset.addValue(100.0, "RMSN", "1");
		lineDataset.addValue(80.0, "RMSN", "2");
		lineDataset.addValue(60.0, "RMSN", "3");
		lineDataset.addValue(50.0, "RMSN", "4");
		lineDataset.addValue(45.0, "RMSN", "5");
		lineDataset.addValue(40.0, "RMSN", "6");
		
		LineVisualiser line = new LineVisualiser(lineDataset, "This is a line chart");
		line.setSize(600, 400);
		line.setVisible(true);
		line.saveToPNG("LineVisualiserTest.png");
	}
}