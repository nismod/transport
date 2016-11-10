/**
 * 
 */
package nismod.transport.network.road;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

/**
 * Tests for the RoadNetwork class
 * @author Milan Lovric
 *
 */
public class RoadNetworkTest {
	
	public static void main( String[] args ) throws IOException	{
				
		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl);
		
		//visualise the shapefiles
		roadNetwork.visualise();
	}

	@Test
	public void test() throws IOException {
		
		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl);
		
		assertTrue("This condition should be true", true);
	}
}
