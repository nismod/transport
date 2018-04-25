package nismod.transport.disruption;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.Edge;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.RouteSetGenerator;

/**
 * Disruption on road links.
 * @author Milan Lovric
 *
 */
public class RoadDisruption extends Disruption {
	
	private final static Logger LOGGER = LogManager.getLogger(RoadDisruption.class);
	
	private List<Edge> listOfRemovedEdges = null;
	private List<Route> listOfRemovedRoutes = null;
	
	/** Constructor.
	 * @param props Properties of the road development intervention.
	 */
	public RoadDisruption (Properties props) {
		
		super(props);
	}
	
	/** Constructor.
	 * @param fileName File with the properties.
	 */
	public RoadDisruption (String fileName) {
		
		super(fileName);
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Disruption#install(java.lang.Object)
	 */
	@Override
	public void install(Object o) {
		
		System.out.println("Implementing road disruption.");
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof RouteSetGenerator) {
			rn = ((RouteSetGenerator)o).getRoadNetwork();
		}
		else if (o instanceof RoadNetworkAssignment) {
			rn = ((RoadNetworkAssignment)o).getRoadNetwork();
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			System.err.println("RoadDisruption installation has received an unexpected type.");
			return;
		}
		
		String listOfDisruptedEdgeIDs = this.props.getProperty("listOfDisruptedEdgeIDs");
		System.out.println(listOfDisruptedEdgeIDs);
		String listOfDisruptedEdgeIDsNoSpace = listOfDisruptedEdgeIDs.replace(" ", ""); //remove space
		String listOfDisruptedEdgeIDsNoTabs = listOfDisruptedEdgeIDsNoSpace.replace("\t", ""); //remove tabs
		System.out.println(listOfDisruptedEdgeIDsNoTabs);
		String[] edgeIDs = listOfDisruptedEdgeIDsNoTabs.split(",");
		
		for (String edgeString: edgeIDs) {
			
			int edgeID = Integer.parseInt(edgeString);
			Edge edge = rn.getEdgeIDtoEdge().get(edgeID); 
			if (edge == null) {
				System.err.println("Cannot find network edge that was specified in the road disruption file.");
				continue;
			} else {
				System.out.printf("Removing edge %d from the network. %n", edge.getID());
				if (this.listOfRemovedEdges == null) this.listOfRemovedEdges = new ArrayList<Edge>();
				this.listOfRemovedEdges.add(edge);
				
				rn.removeRoadLink(edge); //removing it from the road network
				//or just remove from graph?
				
				if (this.listOfRemovedRoutes == null) this.listOfRemovedRoutes = new ArrayList<Route>();
				//for route-choice based, remove from the route set
				if (o instanceof RouteSetGenerator) 
					((RouteSetGenerator)o).removeRoutesWithEdge(edgeID, this.listOfRemovedRoutes);
			}
		}
		
		this.installed = true;
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#uninstall(nismod.transport.demand.DemandModel)
	 */
	@Override
	public void uninstall(Object o) {
		
		System.out.println("Removing road disruption.");
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof RouteSetGenerator) {
			rn = ((RouteSetGenerator)o).getRoadNetwork();
		}
		else if (o instanceof RoadNetworkAssignment) {
			rn = ((RoadNetworkAssignment)o).getRoadNetwork();
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			System.err.println("RoadDisruption uninstallation has received an unexpected type.");
			return;
		}
	
		if (this.listOfRemovedEdges == null || this.listOfRemovedEdges.isEmpty()) {
			System.err.println("RoadDisruption does not have the list of disrupted edges.");
			return;
		} else {
			//restore disrupted edges
			for (Edge edge: this.listOfRemovedEdges) {
				System.out.printf("Restoring edge %d back to the network. %n", edge.getID());
				rn.addRoadLink(edge);
			}
			this.listOfRemovedEdges = null;
		}
		
		if (this.listOfRemovedRoutes == null || this.listOfRemovedRoutes.isEmpty()) {
			System.err.println("RoadDisruption does not have the list of removed routes.");
			return;
		} else {
			//restore disrupted routes in the route set
			for (Route route: this.listOfRemovedRoutes) {
				System.out.printf("Restoring route from %d to %d back to the network. %n", route.getOriginNode().getID(), route.getDestinationNode().getID());
				((RouteSetGenerator)o).addRoute(route);
			}
			this.listOfRemovedRoutes = null;
		}
		
		this.installed = false;
	}
	
	/**
	 * @return List of disrupted edge IDs.
	 */
	public List<Edge> getListOfDisruptedEdgesIDs() {
		
		if (this.listOfRemovedEdges == null || this.listOfRemovedEdges.isEmpty()) {
			System.err.println("There are no disrupted edges.");
			return null;
		}
		return this.listOfRemovedEdges;
	}
	
	/**
	 * @return List of removed routes
	 */
	public List<Route> getListOfRemovedRoutes() {
		
		if (this.listOfRemovedRoutes == null || this.listOfRemovedRoutes.isEmpty()) {
			System.err.println("There are no removed routes.");
			return null;
		}
		return this.listOfRemovedRoutes;
	}
}
