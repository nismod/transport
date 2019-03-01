package nismod.transport.decision;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;

import nismod.transport.demand.DemandModel;

/**
 * Intervention that implements link-based congestion charge
 * which depends on the vehicle type and time of day when trip is made.
 * @author Milan Lovric
 */
public class CongestionCharging extends Intervention {
	
	private final static Logger LOGGER = LogManager.getLogger(CongestionCharging.class);
	
	private String name = null;

	/**
	 * Constructor.
	 * @param props Properties object.
	 */
	public CongestionCharging(Properties props) {
		
		super(props);
	}
	
	/**
	 * Constructor.
	 * @param fileName Path to the input properties file.
	 */
	public CongestionCharging(String fileName) {
		
		super(fileName);
	}

	@Override
	public void install(Object o) {
	
		String name = props.getProperty("name");
		this.name = name;
		
		LOGGER.info("Implementing congestion charging: " + name);
		
		DemandModel dm = null;
		if (o instanceof DemandModel) {
			dm = (DemandModel)o;
		}
		else {
			LOGGER.error("CongestionCharging installation has received an unexpected type. Not installed.");
			return;
		}
		
		//used for array initialisation
		int maximumEdgeID = dm.getRoadNetwork().maximumEdgeID;
	
		String listOfCongestionChargedEdgeIDs = this.props.getProperty("listOfCongestionChargedEdgeIDs");
		System.out.println(listOfCongestionChargedEdgeIDs);
		String listOfCongestionChargedEdgeIDsNoSpace = listOfCongestionChargedEdgeIDs.replace(" ", ""); //remove space
		String listOfCongestionChargedEdgeIDsNoTabs = listOfCongestionChargedEdgeIDsNoSpace.replace("\t", ""); //remove tabs
		System.out.println(listOfCongestionChargedEdgeIDsNoTabs);
		String[] edgeIDs = listOfCongestionChargedEdgeIDsNoTabs.split(",");
		
		//create list of policy affected edge IDs
		List<Integer> listOfEdgeIDs = new ArrayList<Integer>();
		for (String edgeString: edgeIDs) {
			int edgeID = Integer.parseInt(edgeString);
			listOfEdgeIDs.add(edgeID);
		}

		//file with the pricing policy
		String congestionChargingPricing = this.props.getProperty("congestionChargingPricing");

		PricingPolicy policy = null; 
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));

		try {
			policy = new PricingPolicy(name, congestionChargingPricing, maximumEdgeID, listOfEdgeIDs);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		}
		
		LOGGER.debug("Congestion charging pricing policy: {}", policy.getPolicy());
		
		//add policy to all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++) {

			dm.addCongestionCharges(y, policy);
		}

		this.installed = true;
	}

	@Override
	public void uninstall(Object o) {

		
		LOGGER.info("Removing congestion charging.");
		
		DemandModel dm = null;
		if (o instanceof DemandModel) {
			dm = (DemandModel)o;
		}
		else {
			LOGGER.error("CongestionCharging uninstallation has received an unexpected type.");
			return;
		}
	
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));

		//remove congestion charges for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++)
			//dm.setCongestionCharges(y, null);
			dm.removeCongestionCharges(y, this.name);
		
		this.installed = false;
	}
}
