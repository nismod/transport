/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Origin-destination matrix
 * @author Milan Lovric
 *
 */
public class ODMatrix {
	
	private MultiKeyMap matrix;
	
	public ODMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file
	 * @param filePath Path to the input file
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public ODMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("origin");
		//System.out.println("keySet = " + keySet);
		int flow;
		for (CSVRecord record : parser) { 
			System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
			for (String destination: keySet) {
				//System.out.println("Destination zone = " + destination);
				flow = Integer.parseInt(record.get(destination));
				matrix.put(record.get(0), destination, flow);			
			}
		} parser.close(); 
	}
	
	/**
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getFlow(String originZone, String destinationZone) {
		
		return (int) matrix.get(originZone, destinationZone);
	}
	
	/**
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(String originZone, String destinationZone, int flow) {
		
		matrix.put(originZone, destinationZone, flow);
	}
	
	public void printMatrix() {
		
		System.out.println(matrix.toString());
		
	}
	
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
}
