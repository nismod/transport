/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the SkimMatrixFreight class
 * @author Milan Lovric
 *
 */
public class SkimMatrixFreightTest {
	
	@BeforeClass
	public static void initialise() {
	
	    File file = new File("./temp");
	    if (!file.exists()) {
	        if (file.mkdir()) {
	            System.out.println("Temp directory is created.");
	        } else {
	            System.err.println("Failed to create temp directory.");
	        }
	    }
	}

	@Test
	public void test() throws FileNotFoundException, IOException {
		
		//  O   D   V   C
		//-----------------
		//  1   1   1   0.50
		//  1   1   2   1.00
		//  1   1   3   5.00
		//  1   2   1   2.00
		//  1   2   2   5.00
		//  1   2   3   6.00
		//  2   1   1   2.50
		//  2   1   2   1.50
		//  2   1   3   1.00
		
		SkimMatrixFreight skimMatrixFreight = new SkimMatrixFreight();
		
		skimMatrixFreight.setCost(1, 1, 1, 0.50);
		skimMatrixFreight.setCost(1, 1, 2, 1.00);
		skimMatrixFreight.setCost(1, 1, 3, 5.00);
		skimMatrixFreight.setCost(1, 2, 1, 2.00);
		skimMatrixFreight.setCost(1, 2, 2, 5.00);
		skimMatrixFreight.setCost(1, 2, 3, 6.00);
		skimMatrixFreight.setCost(2, 1, 1, 2.50);
		skimMatrixFreight.setCost(2, 1, 2, 1.50);
		skimMatrixFreight.setCost(2, 1, 3, 1.00);
		
		skimMatrixFreight.printMatrix();
		skimMatrixFreight.printMatrixFormatted();
		skimMatrixFreight.printMatrixFormatted("Skim matrix freight");
			
		boolean condition = 		
				skimMatrixFreight.getCost(1, 1, 1) == 0.50 &&
				skimMatrixFreight.getCost(1, 1, 2) == 1.00 &&
				skimMatrixFreight.getCost(1, 1, 3) == 5.00 &&
				skimMatrixFreight.getCost(1, 2, 1) == 2.00 &&
				skimMatrixFreight.getCost(1, 2, 2) == 5.00 &&
				skimMatrixFreight.getCost(1, 2, 3) == 6.00 &&
				skimMatrixFreight.getCost(2, 1, 1) == 2.50 &&
				skimMatrixFreight.getCost(2, 1, 2) == 1.50 &&
				skimMatrixFreight.getCost(2, 1, 3) == 1.00;
	
		assertTrue("All matrix elements are correct", condition);
		
		skimMatrixFreight.saveMatrixFormatted("./temp/skimMatrixFreight.csv");
		
		
		SkimMatrixFreight skimMatrixFreight2 = new SkimMatrixFreight("./src/test/resources/testdata/costSkimMatrixFreight.csv");
			
//		System.out.println(skimMatrixFreight2.getKeySet());
//		for (MultiKey mk: skimMatrixFreight2.getKeySet()) {
//			System.out.println(mk);
//			System.out.println("origin = " + mk.getKey(0));
//			System.out.println("destination = " + mk.getKey(1));
//			System.out.println("vehicleType = " + mk.getKey(2));
//			System.out.println("cost = " + skimMatrixFreight2.getCost((int)mk.getKey(0), (int)mk.getKey(1), (int)mk.getKey(2)));
//		}
	
		skimMatrixFreight2.printMatrixFormatted();
		
		condition = skimMatrixFreight2.getCost(854, 1312, 3) == 1.67 &&
					skimMatrixFreight2.getCost(855, 1312, 2) == 0.01 &&
					skimMatrixFreight2.getCost(1312, 855, 2) == 1.64 &&
					skimMatrixFreight2.getCost(867, 1312, 1) == 0.42 &&
					skimMatrixFreight2.getCost(1312, 867, 1) == 2.88 &&
					skimMatrixFreight2.getCost(867, 867, 1) == 3.00;
					
		assertTrue("Selected matrix elements are correct", condition);
		
		SkimMatrixFreight skimMatrixFreight3 = new SkimMatrixFreight("./temp/skimMatrixFreight.csv");
		
		double diff = skimMatrixFreight3.getAbsoluteDifference(skimMatrixFreight);
		
		final double DELTA = 0.000001;
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		FreightMatrix freightMatrix = new FreightMatrix();
		freightMatrix.setFlow(1, 1, 1, 10);
		freightMatrix.setFlow(1, 1, 2, 10);
		freightMatrix.setFlow(1, 1, 3, 10);
		freightMatrix.setFlow(1, 2, 1, 10);
		freightMatrix.setFlow(1, 2, 2, 10);
		freightMatrix.setFlow(1, 2, 3, 10);
		freightMatrix.setFlow(2, 1, 1, 10);
		freightMatrix.setFlow(2, 1, 2, 10);
		freightMatrix.setFlow(2, 1, 3, 10);

		double averageCost = skimMatrixFreight.getAverageCost();
		double demandWeigthedCost = skimMatrixFreight.getAverageCost(freightMatrix);
		assertEquals("Sum of weighted matrix costs is the same", averageCost, demandWeigthedCost, DELTA);
	}
}
