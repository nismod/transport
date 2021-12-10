/**
 * 
 */
package nismod.transport.decision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;

/**
 * Intervention that creates a new road link between two existing nodes.
 * @author Milan Lovric
 *
 */
public class RoadDevelopment extends Intervention {
	
	private final static Logger LOGGER = LogManager.getLogger();
	
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
		
		LOGGER.info("Implementing road development.");
		
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			LOGGER.error("RoadDevelopment installation has received an unexpected type.");
			return;
		}
		
		int fromNodeId = Integer.parseInt(this.props.getProperty("fromNode"));
		int toNodeId = Integer.parseInt(this.props.getProperty("toNode"));
		boolean biDirectional = Boolean.parseBoolean(this.props.getProperty("biDirectional"));
		int numberOfLanesPerDirection = Integer.parseInt(this.props.getProperty("lanesPerDirection"));
		double length = Double.parseDouble(this.props.getProperty("length"));
		char roadCategory = this.props.getProperty("roadClass").charAt(0);
		int edgeID1 = Integer.parseInt(this.props.getProperty("edgeID1"));
						
		Node fromNode = rn.getNodeIDtoNode()[fromNodeId];
		Node toNode = rn.getNodeIDtoNode()[toNodeId];
		if (fromNode == null || toNode == null) {
			LOGGER.error("Could not find a node where road development should be installed!");
			return;
		}
		
		//create one edge
		Edge newEdge = rn.createNewRoadLink(fromNode, toNode, numberOfLanesPerDirection, roadCategory, length, edgeID1);
		if (newEdge == null) {
			LOGGER.error("Edge creation was not sucessful.");
			return;
		}
		//store edge ID
		this.newEdgeId = newEdge.getID();
		
		//if bidirectional, create the second edge too (in the other direction)
		if(biDirectional) {
			int edgeID2 = Integer.parseInt(this.props.getProperty("edgeID2"));
			Edge newEdge2 = rn.createNewRoadLink(toNode, fromNode, numberOfLanesPerDirection, roadCategory, length, edgeID2);
			if (newEdge2 == null) {
				LOGGER.error("Second edge creation was not sucessful.");
				return;
			}
			//update map that maps edgeId to the edgeID of other direction
			rn.getEdgeIDtoOtherDirectionEdgeID()[newEdge.getID()] = newEdge2.getID();
			rn.getEdgeIDtoOtherDirectionEdgeID()[newEdge2.getID()] = newEdge.getID();
			
			//store edge ID
			this.newEdgeId2 = newEdge2.getID();
		}
		
		//if installed within DemandModel, also check if new routes should be generated
		if (o instanceof DemandModel) {
			
			String LADs = this.props.getProperty("LADs");
			if (LADs != null) {
				
				DemandModel dm = (DemandModel)o;
				int startYear = this.getStartYear();
				
				LOGGER.debug("Found LADs for which routes will need to be re-generated in year {}:", startYear);

				String LADsNoSpace = LADs.replace(" ", ""); //remove space
				String LADsNoTabs = LADsNoSpace.replace("\t", ""); //remove tabs
				String[] LADsSplit = LADsNoTabs.split(",");
				List<String> LADsList = Arrays.asList(LADsSplit);
				
				LOGGER.debug(LADsList);
				
				//check if RoadNetwork is aware of the provided LAD codes
				for (String lad: LADsList)
					if (!rn.getZoneToNodes().containsKey(lad))
						LOGGER.error("Unrecognised LAD code in RoadDevelopment intervention file: " + lad);
							
				List<List<String>> list = dm.getListsOfLADsForNewRouteGeneration().get(startYear);
				if (list == null) {
					list = new ArrayList<List<String>>();
					dm.getListsOfLADsForNewRouteGeneration().put(startYear, list);
				}
				//store list of LADs
				list.add(LADsList);
			}
		}
		
		this.installed = true;
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#uninstall(nismod.transport.demand.DemandModel)
	 */
	@Override
	public void uninstall(Object o) {
		
		LOGGER.info("Removing road development.");
		
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			LOGGER.error("RoadDevelopment installation has received an unexpected type.");
			return;
		}
	
		if (this.newEdgeId == null) {
			LOGGER.warn("RoadDevelopment does not have ID of the edge that needs to be removed.");
			return;
		} else 
			rn.removeRoadLink(rn.getEdgeIDtoEdge()[this.newEdgeId]);
		
		//if other edge exists, remove it too
		if (this.newEdgeId2 != null) 
			rn.removeRoadLink(rn.getEdgeIDtoEdge()[this.newEdgeId2]);
		
		this.installed = false;
	}
	
	/**
	 * @return Edge ID of the developed road link.
	 */
	public Integer getDevelopedEdgeID() {
		
		if (this.newEdgeId == null) {
			LOGGER.warn("Unknown edge ID of developed road link!");
			return null;
		}
		return this.newEdgeId;
	}
	
	/**
	 * @return Edge ID of the developed road link (in other direction)
	 */
	public Integer getDevelopedEdgeID2() {
		
		if (this.newEdgeId2 == null) {
			LOGGER.warn("Unknown edge ID of developed road link in second direction!");
			return null;
		}
		return this.newEdgeId2;
	}
}
