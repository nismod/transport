package nismod.transport.air;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.rail.RailDemandModel.ElasticityArea;
import nismod.transport.rail.RailStation.RailModeType;
import nismod.transport.utility.ConfigReader;

/**
 * Test class for the InternodalPassengerDemand class.
 * @author Milan Lovric
  */
public class InternodalPassengerDemandTest {
	
	@Test
	public void test() throws IOException {
		
		String path = "./src/test/resources/testdata/csvfiles/DomesticInternodalPassengerBaseYearDemandTest.csv";
		String path2 = "./src/test/resources/testdata/csvfiles/InternationalInternodalPassengerBaseYearDemandTest.csv";
		
		InternodalPassengerDemand domestic = new DomesticInternodalPassengerDemand(path);
		domestic.printDemand();
		
		InternodalPassengerDemand foreign = new InternationalInternodalPassengerDemand(path2);
		foreign.printDemand();

	}
}
