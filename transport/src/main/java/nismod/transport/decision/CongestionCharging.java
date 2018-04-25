package nismod.transport.decision;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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
 * that depends on vehicle type and time of day.
 * @author Milan Lovric
 */
public class CongestionCharging extends Intervention {
	
	private final static Logger LOGGER = LogManager.getLogger(CongestionCharging.class);
	
	private String name = null;

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
	
		String name = props.getProperty("name");
		this.name = name;
		
		System.out.println("Implementing congestion charging: " + name);
		
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
		
		//double congestionCharge = Double.parseDouble(this.props.getProperty("congestionCharge"));
		String congestionChargingPricing = this.props.getProperty("congestionChargingPricing");

		MultiKeyMap congestionCharge = null; 
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));

		try {
			congestionCharge = this.readCongestionChargeFile(congestionChargingPricing);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(congestionCharge.toString());
		
		//set congestion charge for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++) {

			MultiKeyMap congestionCharges = new MultiKeyMap();

			for (Object mk: congestionCharge.keySet()) {

				VehicleType vht = (VehicleType) ((MultiKey)mk).getKey(0);
				TimeOfDay hour = (TimeOfDay) ((MultiKey)mk).getKey(1);

				double charge = (double) congestionCharge.get(vht, hour);

				HashMap<Integer, Double> linkCharges = new HashMap<Integer, Double>();
				for (String edgeString: edgeIDs) {
					int edgeID = Integer.parseInt(edgeString);
					linkCharges.put(edgeID, charge);
				}

				congestionCharges.put(vht, hour, linkCharges);
			}

			//dm.setCongestionCharges(y, congestionCharges);
			dm.addCongestionCharges(y, name, congestionCharges);
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

		//remove congestion charges for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++)
			//dm.setCongestionCharges(y, null);
			dm.removeCongestionCharges(y, this.name);
		
		this.installed = false;
	}
	
	
	/**
	 * Reads congestion charge file which contains charges that depend on vehicle type and time of day (hour).
	 * @param fileName File name.
	 * @return Map with congestion charges.
	 */
	public MultiKeyMap readCongestionChargeFile (String fileName) throws FileNotFoundException, IOException {

		//HashMap<Integer, HashMap<String, Double>> map = new HashMap<Integer, HashMap<String, Double>>();
		MultiKeyMap map = new MultiKeyMap();
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("vehicleType");
		//System.out.println("keySet = " + keySet);
		Double charge;
		for (CSVRecord record : parser) {
			//System.out.println(record);
			VehicleType vht = VehicleType.valueOf(record.get(0));
			for (String time: keySet) {
				//System.out.println("Time of day = " + time);
				charge = Double.parseDouble(record.get(time));  
				map.put(vht, TimeOfDay.valueOf(time), charge);			
			}
		}
		
		parser.close(); 

		return map;
	}
}
