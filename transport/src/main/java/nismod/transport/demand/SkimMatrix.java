/**
 * 
 */
package nismod.transport.demand;

import java.util.List;

/**
 * Skim matrix for storing inter-zonal travel times or costs (for passenger vehicles).
 * @author Milan Lovric
 *
 */
public interface SkimMatrix {
	
	/**
	 * Gets cost for a given origin-destination pair using ONS codes.
	 * @param originZone Origin zone ONS code.
	 * @param destinationZone Destination zone ONS code.
	 * @return Origin-destination cost.
	 */
	public double getCost(String originZone, String destinationZone);
	
	/**
	 * Sets cost for a given origin-destination pair using ONS codes.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(String originZone, String destinationZone, double cost);
	
	/**
	 * Gets cost for a given origin-destination pair using int zone IDs.
	 * @param originZone Origin zone ID.
	 * @param destinationZone Destination zone ID.
	 * @return Origin-destination cost.
	 */
	public double getCost(int originZoneID, int destinationZoneID);
	
	/**
	 * Sets cost for a given origin-destination pair using int zone IDs.
	 * @param originZoneID Origin zone ID.
	 * @param destinationZoneID Destination zone ID.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(int originZoneID, int destinationZoneID, double cost);
	
	/**
	 * Gets the unsorted list of origins.
	 * @return List of origin zones.
	 */
	public List<String> getUnsortedOrigins();
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destination zones.
	 */
	public List<String> getUnsortedDestinations();
	
	/**
	 * Gets the sorted list of origins.
	 * @return List of origin zones.
	 */
	public List<String> getSortedOrigins();
	
	/**
	 * Gets the sroted list of destinations.
	 * @return List of destination zones.
	 */
	public List<String> getSortedDestinations();
	
	/**
	 * Prints the matrix.
	 */
	public void printMatrix();
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted();
	
	/**
	 * Prints the matrix as a formatted table, with a print message.
	 * @param s Print message
	 */
	public void printMatrixFormatted(String s);
	
	/**
	 * Saves the matrix into a csv file (matrix format).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile);
	
	/**
	 * Saves the matrix into a csv file (list format).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormattedList(String outputFile);
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(SkimMatrix other);
}
