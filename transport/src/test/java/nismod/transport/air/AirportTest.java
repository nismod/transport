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
 * Test class for the Airport class.
 * @author Milan Lovric
  */
public class AirportTest {
	
	@Test
	public void test() throws IOException {

		Airport airport = new DomesticAirport("LHR", "9200LHR0", "London Heathrow", "London Heathrow Airport", "London Heathrow Airport", 507546, 176188, -0.4528908, 51.47444403, "E09000017", "Hillingdon", 90000000, 480000);
		System.out.println(airport);
		
		Airport airport2 = new InternationalAirport("CDG", "Paris (Charles da Gaulle)", "Charles de Gaulle International Airport", 2.55, 49.012798, "FR", "EU", 90000000, 480000);
		System.out.println(airport2);
	}
}
