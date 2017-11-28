package nismod.transport.decision;

import java.util.HashMap;
import java.util.Properties;

import nismod.transport.demand.DemandModel;

public class CongestionCharging extends Intervention {

	/**
	 * @param props
	 */
	public CongestionCharging(Properties props) {
		
		super(props);
	}
	
	/**
	 * @param fileName
	 */
	public CongestionCharging(String fileName) {
		
		super(fileName);
	}

	@Override
	public void install(Object o) {
	
		System.out.println("Implementing congestion charging.");
		
		DemandModel dm = null;
		if (o instanceof DemandModel) {
			dm = (DemandModel)o;
		}
		else {
			System.err.println("CongestionCharging installation has received an unexpected type.");
			return;
		}
	
		String listOfCongestionChargedEdgeIDs = this.props.getProperty("listOfCongestionChargedEdgeIDs");
		System.out.println(listOfCongestionChargedEdgeIDs);
		String listOfCongestionChargedEdgeIDsNoSpace = listOfCongestionChargedEdgeIDs.replace(" ", ""); //remove space
		String listOfCongestionChargedEdgeIDsNoTabs = listOfCongestionChargedEdgeIDsNoSpace.replace("\t", ""); //remove tabs
		System.out.println(listOfCongestionChargedEdgeIDsNoTabs);
		String[] edgeIDs = listOfCongestionChargedEdgeIDsNoTabs.split(",");
		
		double congestionCharge = Double.parseDouble(this.props.getProperty("congestionCharge"));
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));

		//set congestion charge for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++) {

			HashMap<Integer, Double> linkCharges = new HashMap<Integer, Double>();
			for (String edgeString: edgeIDs) {
				int edgeID = Integer.parseInt(edgeString);
				linkCharges.put(edgeID, congestionCharge);
			}

			dm.setCongestionCharges(y, linkCharges);
		}
	
		this.installed = true;
	}

	@Override
	public void uninstall(Object o) {

		
		System.out.println("Removing congestion charging.");
		
		DemandModel dm = null;
		if (o instanceof DemandModel) {
			dm = (DemandModel)o;
		}
		else {
			System.err.println("CongestionCharging uninstallation has received an unexpected type.");
			return;
		}
	
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));

		//set no charge for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++)
			dm.setCongestionCharges(y, null);
		
		this.installed = false;
	}
}
