/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for the ODMatrix class
 * @author Milan Lovric
 *
 */
public class ODMatrixTest {

	@Test
	public void test() {
		
		//   1   2   3   4
		//-----------------
		//1 123 234 345 456
		//2 321 432 543 654
		//3 987 876 765 654
		//4 456 567 678 789
		
		ODMatrix passengerODMatrix = new ODMatrix();
		
		passengerODMatrix.setFlow(1, 1, 123);
		passengerODMatrix.setFlow(1, 2, 234);
		passengerODMatrix.setFlow(1, 3, 345);
		passengerODMatrix.setFlow(1, 4, 456);
		passengerODMatrix.setFlow(2, 1, 321);
		passengerODMatrix.setFlow(2, 2, 432);
		passengerODMatrix.setFlow(2, 3, 543);
		passengerODMatrix.setFlow(2, 4, 654);
		passengerODMatrix.setFlow(3, 1, 987);
		passengerODMatrix.setFlow(3, 2, 876);
		passengerODMatrix.setFlow(3, 3, 765);
		passengerODMatrix.setFlow(3, 4, 654);
		passengerODMatrix.setFlow(4, 1, 456);
		passengerODMatrix.setFlow(4, 2, 567);
		passengerODMatrix.setFlow(4, 3, 678);
		passengerODMatrix.setFlow(4, 4, 789);
	
		passengerODMatrix.printMatrix();
			
		boolean condition = 		
				passengerODMatrix.getFlow(1, 1) == 123 &&
				passengerODMatrix.getFlow(1, 2) == 234 &&
				passengerODMatrix.getFlow(1, 3) == 345 &&
				passengerODMatrix.getFlow(1, 4) == 456 &&
				passengerODMatrix.getFlow(2, 1) == 321 &&
				passengerODMatrix.getFlow(2, 2) == 432 &&
				passengerODMatrix.getFlow(2, 3) == 543 &&
				passengerODMatrix.getFlow(2, 4) == 654 &&
				passengerODMatrix.getFlow(3, 1) == 987 &&
				passengerODMatrix.getFlow(3, 2) == 876 &&
				passengerODMatrix.getFlow(3, 3) == 765 &&
				passengerODMatrix.getFlow(3, 4) == 654 &&
				passengerODMatrix.getFlow(4, 1) == 456 &&
				passengerODMatrix.getFlow(4, 2) == 567 &&
				passengerODMatrix.getFlow(4, 3) == 678 &&
				passengerODMatrix.getFlow(4, 4) == 789;
	
		assertTrue("All matrix elements are correct", condition);
	}
}
