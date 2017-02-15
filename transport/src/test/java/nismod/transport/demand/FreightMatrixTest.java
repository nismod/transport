/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.junit.Test;

/**
 * Tests for the ODMatrix class
 * @author Milan Lovric
 *
 */
public class FreightMatrixTest {

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
		
		freightMatrix.printMatrix();
			
		boolean condition = 		
				freightMatrix.getFlow(1, 1, 1) == 50 &&
				freightMatrix.getFlow(1, 1, 2) == 10 &&
				freightMatrix.getFlow(1, 1, 3) == 500 &&
				freightMatrix.getFlow(1, 2, 1) == 20 &&
				freightMatrix.getFlow(1, 2, 2) == 50 &&
				freightMatrix.getFlow(1, 2, 3) == 60 &&
				freightMatrix.getFlow(2, 1, 1) == 250 &&
				freightMatrix.getFlow(2, 1, 2) == 150 &&
				freightMatrix.getFlow(2, 1, 3) == 1;
	
		assertTrue("All matrix elements are correct", condition);
		
		FreightMatrix freightMatrix2 = new FreightMatrix("./src/test/resources/testdata/freightMatrix.csv");
			
		System.out.println(freightMatrix2.getKeySet());
		for (MultiKey mk: freightMatrix2.getKeySet()) {
			System.out.println(mk);
			System.out.println("origin = " + mk.getKey(0));
			System.out.println("destination = " + mk.getKey(1));
			System.out.println("vehicleType = " + mk.getKey(2));
			System.out.println("flow = " + freightMatrix2.getFlow((int)mk.getKey(0), (int)mk.getKey(1), (int)mk.getKey(2)));
		}
	
		freightMatrix2.printMatrixFormatted();
		
		condition = freightMatrix2.getFlow(854, 1312, 3) == 3 &&
					freightMatrix2.getFlow(855, 1312, 2) == 117 &&
					freightMatrix2.getFlow(1312, 855, 2) == 235 &&
					freightMatrix2.getFlow(867, 1312, 1) == 1 &&
					freightMatrix2.getFlow(1312, 867, 1) == 2 &&
					freightMatrix2.getFlow(867, 867, 1) == 95;
					
		assertTrue("All matrix elements are correct", condition);	
	}
}
