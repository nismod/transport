/**
 * 
 */
package nismod.transport.demand;

import java.util.List;

/**
 * Skim matrix for storing inter-zonal travel times or costs (for freight vehicles).
 * @author Milan Lovric
 *
 */
public interface SkimMatrixFreight {
	
	public final static int MAX_FREIGHT_ZONE_ID = 1388;
	public final static int MAX_VEHICLE_ID = 3;
	
	/**
	 * Gets cost for a given origin-destination pair and a vehicle type.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param vehicleType Vehicle type.
	 * @return Origin-destination cost.
	 */
	public double getCost(int originZone, int destinationZone, int vehicleType);
	
	/**
	 * Sets cost for a given origin-destination pair and a vehicle type.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param vehicleType Vehicle type.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(int originZone, int destinationZone, int vehicleType, double cost);
	
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
	 * Saves the matrix into a csv file (list format for freight).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile);
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(SkimMatrixFreight other);
	
	/**
	 * Gets average cost.
	 * @return Average cost.
	 */
	public double getAverageCost();
	
	/**
	 * Gets average cost weighted by the freight demand.
	 * @param demand Freight OD matrix.
	 * @return Average cost weighted by freight matrix.
	 */
	public double getAverageCost(FreightMatrix demand);
}
