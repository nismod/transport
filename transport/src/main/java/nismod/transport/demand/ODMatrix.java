/**
 * 
 */
package nismod.transport.demand;

import org.apache.commons.collections4.map.MultiKeyMap;

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
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getFlow(int originZone, int destinationZone) {
		
		return (int) matrix.get(originZone, destinationZone);
	}
	
	/**
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(int originZone, int destinationZone, int flow) {
		
		matrix.put(originZone, destinationZone, flow);
	}
	
	public void printMatrix() {
		
		System.out.println(matrix.toString());
		
	}
}
