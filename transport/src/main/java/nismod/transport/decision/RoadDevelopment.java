/**
 * 
 */
package nismod.transport.decision;

import java.util.Properties;
import java.util.logging.Logger;

import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;

/**
 * Intervention that creates a new road link between two existing nodes.
 * @author Milan Lovric
 *
 */
public class RoadDevelopment extends Intervention {
	
	private final static Logger LOGGER = Logger.getLogger(RoadDevelopment.class.getName());
	
	private Integer newEdgeId = null;
	private Integer newEdgeId2 = null;
	
	/** Constructor.
	 * @param props Properties of the road development intervention.
	 */
	public RoadDevelopment (Properties props) {
		
		super(props);
	}
	
	/** Constructor.
	 * @param fileName File with the properties.
	 */
	public RoadDevelopment (String fileName) {
		
		super(fileName);
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#install(java.lang.Object)
	 */
	@Override
	public void install(Object o) {
		
		System.out.println("Implementing road development.");
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			System.err.println("RoadDevelopment installation has received an unexpected type.");
			return;
		}
		
		int fromNodeId = Integer.parseInt(this.props.getProperty("fromNode"));
		int toNodeId = Integer.parseInt(this.props.getProperty("toNode"));
		boolean biDirectional = Boolean.parseBoolean(this.props.getProperty("biDirectional"));
		int numberOfLanesPerDirection = Integer.parseInt(this.props.getProperty("lanesPerDirection"));
		double length = Double.parseDouble(this.props.getProperty("length"));
		char roadCategory = this.props.getProperty("roadCategory").charAt(0);
				
		Node fromNode = rn.getNodeIDtoNode().get(fromNodeId);
		Node toNode = rn.getNodeIDtoNode().get(toNodeId);
		if (fromNode == null || toNode == null) {
			System.err.println("Could not find a node where road development should be installed!");
			return;
		}
		
		//create one edge
		Edge newEdge = rn.createNewRoadLink(fromNode, toNode, numberOfLanesPerDirection, roadCategory, length);
		if (newEdge == null) {
			System.err.println("Edge creation was not sucessful");
			return;
		}
		//store edge ID
		this.newEdgeId = newEdge.getID();
		
		//if bidirectional, create the second edge too (in the other direction)
		if(biDirectional) {
			Edge newEdge2 = rn.createNewRoadLink(toNode, fromNode, numberOfLanesPerDirection, roadCategory, length);
			if (newEdge2 == null) {
				System.err.println("Second edge creation was not sucessful");
				return;
			}
			//update map that maps edgeId to the edgeID of other direction
			rn.getEdgeIDtoOtherDirectionEdgeID().put(newEdge.getID(), newEdge2.getID());
			rn.getEdgeIDtoOtherDirectionEdgeID().put(newEdge2.getID(), newEdge.getID());
			
			//store edge ID
			this.newEdgeId2 = newEdge2.getID();
		}
		
		this.installed = true;
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#uninstall(nismod.transport.demand.DemandModel)
	 */
	@Override
	public void uninstall(Object o) {
		
		System.out.println("Removing road development.");
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			System.err.println("RoadDevelopment installation has received an unexpected type.");
			return;
		}
	
		if (this.newEdgeId == null) {
			System.err.println("RoadDevelopment does not have ID of the edge that needs to be removed.");
			return;
		} else 
			rn.removeRoadLink(rn.getEdgeIDtoEdge().get(this.newEdgeId));
		
		//if other edge exists, remove it too
		if (this.newEdgeId2 != null) 
			rn.removeRoadLink(rn.getEdgeIDtoEdge().get(this.newEdgeId2));
		
		this.installed = false;
	}
	
	/**
	 * @return Edge ID of the developed road link.
	 */
	public Integer getDevelopedEdgeID() {
		
		if (this.newEdgeId == null) {
			System.err.println("Unknown edge ID of developed road link!");
			return null;
		}
		return this.newEdgeId;
	}
	
	/**
	 * @return Edge ID of the developed road link (in other direction)
	 */
	public Integer getDevelopedEdgeID2() {
		
		if (this.newEdgeId2 == null) {
			System.err.println("Unknown edge ID of developed road link in second direction!");
			return null;
		}
		return this.newEdgeId2;
	}
}
