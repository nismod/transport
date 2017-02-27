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
public class ODMatrixTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		
		//   1   2   3   4
		//-----------------
		//1 123 234 345 456
		//2 321 432 543 654
		//3 987 876 765 654
		//4 456 567 678 789
		
		ODMatrix passengerODMatrix = new ODMatrix();
		
		passengerODMatrix.setFlow("1", "1", 123);
		passengerODMatrix.setFlow("1", "2", 234);
		passengerODMatrix.setFlow("1", "3", 345);
		passengerODMatrix.setFlow("1", "4", 456);
		passengerODMatrix.setFlow("2", "1", 321);
		passengerODMatrix.setFlow("2", "2", 432);
		passengerODMatrix.setFlow("2", "3", 543);
		passengerODMatrix.setFlow("2", "4", 654);
		passengerODMatrix.setFlow("3", "1", 987);
		passengerODMatrix.setFlow("3", "2", 876);
		passengerODMatrix.setFlow("3", "3", 765);
		passengerODMatrix.setFlow("3", "4", 654);
		passengerODMatrix.setFlow("4", "1", 456);
		passengerODMatrix.setFlow("4", "2", 567);
		passengerODMatrix.setFlow("4", "3", 678);
		passengerODMatrix.setFlow("4", "4", 987);
	
		passengerODMatrix.printMatrixFormatted();
		
		passengerODMatrix.setFlow("4",  "4", 0);
		passengerODMatrix.printMatrixFormatted();
		
		boolean condition = 		
				passengerODMatrix.getFlow("1", "1") == 123 &&
				passengerODMatrix.getFlow("1", "2") == 234 &&
				passengerODMatrix.getFlow("1", "3") == 345 &&
				passengerODMatrix.getFlow("1", "4") == 456 &&
				passengerODMatrix.getFlow("2", "1") == 321 &&
				passengerODMatrix.getFlow("2", "2") == 432 &&
				passengerODMatrix.getFlow("2", "3") == 543 &&
				passengerODMatrix.getFlow("2", "4") == 654 &&
				passengerODMatrix.getFlow("3", "1") == 987 &&
				passengerODMatrix.getFlow("3", "2") == 876 &&
				passengerODMatrix.getFlow("3", "3") == 765 &&
				passengerODMatrix.getFlow("3", "4") == 654 &&
				passengerODMatrix.getFlow("4", "1") == 456 &&
				passengerODMatrix.getFlow("4", "2") == 567 &&
				passengerODMatrix.getFlow("4", "3") == 678 &&
				passengerODMatrix.getFlow("4", "4") == 0;
	
		assertTrue("All matrix elements are correct", condition);
		
		ODMatrix passengerODMatrix2 = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		passengerODMatrix2.printMatrixFormatted();
//		System.out.println(passengerODMatrix2.getKeySet());
//		for (MultiKey mk: passengerODMatrix2.getKeySet()) {
//			System.out.println(mk);
//			System.out.println("origin = " + mk.getKey(0));
//			System.out.println("destination = " + mk.getKey(1));
//			System.out.println("flow = " + passengerODMatrix2.getFlow((String)mk.getKey(0), (String)mk.getKey(1)));
//		}
		
		condition = passengerODMatrix2.getFlow("E06000045", "E06000045") == 5000 &&
					passengerODMatrix2.getFlow("E06000045", "E07000086") == 5500 &&
					passengerODMatrix2.getFlow("E06000045", "E07000091") == 2750 &&
					passengerODMatrix2.getFlow("E06000045", "E06000046") == 150 &&
					passengerODMatrix2.getFlow("E07000086", "E06000045") == 6500 &&
					passengerODMatrix2.getFlow("E07000086", "E07000086") == 5500 &&
					passengerODMatrix2.getFlow("E07000086", "E07000091") == 900 &&
					passengerODMatrix2.getFlow("E07000086", "E06000046") == 120 &&
					passengerODMatrix2.getFlow("E07000091", "E06000045") == 4560 &&
					passengerODMatrix2.getFlow("E07000091", "E07000086") == 1400 &&
					passengerODMatrix2.getFlow("E07000091", "E07000091") == 6000 &&
					passengerODMatrix2.getFlow("E07000091", "E06000046") == 100 &&
					passengerODMatrix2.getFlow("E06000046", "E06000045") == 200 &&
					passengerODMatrix2.getFlow("E06000046", "E07000086") == 150 &&
					passengerODMatrix2.getFlow("E06000046", "E07000091") == 90 &&
					passengerODMatrix2.getFlow("E06000046", "E06000046") == 1000;
				
		assertTrue("All matrix elements are correct", condition);	
	}
}
