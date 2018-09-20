/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the SkimMatrix class
 * @author Milan Lovric
 *
 */
public class SkimMatrixTest {
	
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
		
		SkimMatrix skimMatrix = new SkimMatrix();
		
		skimMatrix.setCost("E06000045", "E06000045", 6.2);
		skimMatrix.setCost("E06000045", "E07000086", 5.5);
		skimMatrix.setCost("E06000045", "E07000091", 12.55); 
		skimMatrix.setCost("E06000045", "E06000046", 4.8);
		skimMatrix.setCost("E07000086", "E06000045", 6.5);
		skimMatrix.setCost("E07000086", "E07000086", 5.5); 
		skimMatrix.setCost("E07000086", "E07000091", 9.0);
		skimMatrix.setCost("E07000086", "E06000046", 12.0);
		skimMatrix.setCost("E07000091", "E06000045", 4.56);
		skimMatrix.setCost("E07000091", "E07000086", 14.0);
		skimMatrix.setCost("E07000091", "E07000091", 6.0);
		skimMatrix.setCost("E07000091", "E06000046", 10.0);
		skimMatrix.setCost("E06000046", "E06000045", 20.43);
		skimMatrix.setCost("E06000046", "E07000086", 15.12);
		skimMatrix.setCost("E06000046", "E07000091", 9.4);
		//skimMatrix.setCost("E06000046", "E06000046", 6.2);
		
		skimMatrix.printMatrixFormatted();
		skimMatrix.saveMatrixFormatted("./temp/skimMatrix.csv");
		
		SkimMatrix skimMatrix2 = new SkimMatrix("./temp/skimMatrix.csv");
		double diff = skimMatrix2.getAbsoluteDifference(skimMatrix);
		
		final double DELTA = 0.000001;
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		double averageCost = skimMatrix2.getAverageCost();
		double sumOfCosts = skimMatrix2.getSumOfCosts();
		skimMatrix2.printMatrixFormatted();
		skimMatrix2.printMatrixFormatted("Skim matrix");
		
		ODMatrix odMatrix = new ODMatrix();
		odMatrix.setFlow("E06000045", "E06000045", 10);
		odMatrix.setFlow("E06000045", "E07000086", 10);
		odMatrix.setFlow("E06000045", "E07000091", 10); 
		odMatrix.setFlow("E06000045", "E06000046", 10);
		odMatrix.setFlow("E07000086", "E06000045", 10);
		odMatrix.setFlow("E07000086", "E07000086", 10); 
		odMatrix.setFlow("E07000086", "E07000091", 10);
		odMatrix.setFlow("E07000086", "E06000046", 10);
		odMatrix.setFlow("E07000091", "E06000045", 10);
		odMatrix.setFlow("E07000091", "E07000086", 10);
		odMatrix.setFlow("E07000091", "E07000091", 10);
		odMatrix.setFlow("E07000091", "E06000046", 10);
		odMatrix.setFlow("E06000046", "E06000045", 10);
		odMatrix.setFlow("E06000046", "E07000086", 10);
		odMatrix.setFlow("E06000046", "E07000091", 10);
		odMatrix.setFlow("E06000046", "E06000046", 10);
		
		double demandCosts = skimMatrix2.getSumOfCosts(odMatrix);
		assertEquals("Sum of weighted matrix costs is the same", sumOfCosts * 10, demandCosts, DELTA);
		
		double demandWeigthedCost = skimMatrix2.getAverageCost(odMatrix);
		assertEquals("Sum of weighted matrix costs is the same", averageCost, demandWeigthedCost, DELTA);
		
		
	}
}
