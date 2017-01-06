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
	public Double getFlow(int originZone, int destinationZone) {
		
		return (Double) matrix.get(originZone, destinationZone);
	}
	
	/**
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(int originZone, int destinationZone, double flow) {
		
		matrix.put(originZone, destinationZone, flow);
	}
}
