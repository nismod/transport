/**
 * 
 */
package nismod.transport.demand;

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
	}
}
