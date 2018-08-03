package nismod.transport.visualisation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.sanselan.ImageWriteException;
import org.jfree.data.general.DefaultPieDataset;

import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;

/**
 * @author Milan Lovric
 *
 */
public class PieChartVisualiserTest {
	
	public static void main( String[] args ) throws IOException, ImageWriteException	{
		
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
		
		final String configFile = "./src/main/full/config/config.properties";
		//final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
		HashMap<EngineType, Double> engineMap = InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile).get(BASE_YEAR).get(VehicleType.CAR);
		
		pieDataset = new DefaultPieDataset();
		for (Entry<EngineType, Double> entry: engineMap.entrySet())
			pieDataset.setValue(entry.getKey().name(), entry.getValue());
		PieChartVisualiser pie3 = new PieChartVisualiser(pieDataset, "Engine type fractions", "BrBG", true);	
		pie3.setVisible(true);
		pie3.saveToPNG("EnginTypeFractions.png");
	}
}