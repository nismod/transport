/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.sanselan.ImageWriteException;
import org.junit.Test;

/**
 * Tests for the FreightMatrix class
 * @author Milan Lovric
 *
 */
public class FreightMatrixTest {
	
	public static void main( String[] args ) throws FileNotFoundException, IOException, ImageWriteException {
		
		FreightMatrix fm = FreightMatrix.createUnitBYFMMatrix();
		//fm.printMatrixFormatted("Unit freight matrix:");

		System.out.println("Origins: " + fm.getSortedOrigins().size());
		System.out.println("Destinations: " + fm.getUnsortedDestinations().size());
		System.out.println("Total flow: " + fm.getTotalIntFlow());
		fm.saveMatrixFormatted("unitFreightMatrix.csv");
	}

	@Test
	public void test() throws FileNotFoundException, IOException {
		
		//  O   D   V   F
		//-----------------
		//  1   1   1   50
		//  1   1   2   10
		//  1   1   3   500
		//  1   2   1   20
		//  1   2   2   50
		//  1   2   3   60
		//  2   1   1   250
		//  2   1   2   150
		//  2   1   3   1
		
		FreightMatrix freightMatrix = new FreightMatrix();
		
		freightMatrix.setFlow(1, 1, 1, 50);
		freightMatrix.setFlow(1, 1, 2, 10);
		freightMatrix.setFlow(1, 1, 3, 500);
		freightMatrix.setFlow(1, 2, 1, 20);
		freightMatrix.setFlow(1, 2, 2, 50);
		freightMatrix.setFlow(1, 2, 3, 60);
		freightMatrix.setFlow(2, 1, 1, 250);
		freightMatrix.setFlow(2, 1, 2, 150);
		freightMatrix.setFlow(2, 1, 3, 1);
		
		freightMatrix.printMatrixFormatted();
		
		freightMatrix.setFlow(2, 1, 3, 0);
		freightMatrix.printMatrixFormatted();
		
		freightMatrix.getScaledMatrix(2.0).printMatrixFormatted();
			
		boolean condition = 		
				freightMatrix.getFlow(1, 1, 1) == 50 &&
				freightMatrix.getFlow(1, 1, 2) == 10 &&
				freightMatrix.getFlow(1, 1, 3) == 500 &&
				freightMatrix.getFlow(1, 2, 1) == 20 &&
				freightMatrix.getFlow(1, 2, 2) == 50 &&
				freightMatrix.getFlow(1, 2, 3) == 60 &&
				freightMatrix.getFlow(2, 1, 1) == 250 &&
				freightMatrix.getFlow(2, 1, 2) == 150 &&
				freightMatrix.getFlow(2, 1, 3) == 0;
	
		assertTrue("All matrix elements are correct", condition);
		
		FreightMatrix freightMatrix2 = new FreightMatrix("./src/test/resources/testdata/csvfiles/freightMatrix.csv");
			
//		System.out.println(freightMatrix2.getKeySet());
//		for (MultiKey mk: freightMatrix2.getKeySet()) {
//			System.out.println(mk);
//			System.out.println("origin = " + mk.getKey(0));
//			System.out.println("destination = " + mk.getKey(1));
//			System.out.println("vehicleType = " + mk.getKey(2));
//			System.out.println("flow = " + freightMatrix2.getFlow((int)mk.getKey(0), (int)mk.getKey(1), (int)mk.getKey(2)));
//		}
	
		freightMatrix2.printMatrixFormatted();
		
		System.out.println("Scaled freight matrix: ");
		double freightScalingFactor = 0.84;
		freightMatrix2.getScaledMatrix(freightScalingFactor).printMatrixFormatted();
		
		condition = freightMatrix2.getFlow(854, 1312, 3) == 3 &&
					freightMatrix2.getFlow(855, 1312, 2) == 117 &&
					freightMatrix2.getFlow(1312, 855, 2) == 235 &&
					freightMatrix2.getFlow(867, 1312, 1) == 1 &&
					freightMatrix2.getFlow(1312, 867, 1) == 2 &&
					freightMatrix2.getFlow(867, 867, 1) == 95;
					
		assertTrue("All matrix elements are correct", condition);
		
		assertEquals("Total flow is correct", 7772, freightMatrix2.getTotalIntFlow());
		
		System.out.println(freightMatrix2.getSortedOrigins());
		System.out.println(freightMatrix2.getUnsortedOrigins());
		System.out.println(freightMatrix2.getSortedDestinations());
		System.out.println(freightMatrix2.getUnsortedDestinations());
	}
}
