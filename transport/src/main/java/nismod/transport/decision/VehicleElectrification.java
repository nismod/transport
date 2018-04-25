/**
 * 
 */
package nismod.transport.decision;

import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jfree.data.general.DefaultPieDataset;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * @author Milan Lovric
 *
 */
public class VehicleElectrification extends Intervention {
	
	private final static Logger LOGGER = LogManager.getLogger(VehicleElectrification.class.getName());

	/**
	 * @param props
	 */
	public VehicleElectrification(Properties props) {
		
		super(props);
	}
	
	/**
	 * @param fileName
	 */
	public VehicleElectrification(String fileName) {
		
		super(fileName);
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#install(java.lang.Object)
	 * Installs engine fractions for all years in the [startYear, endYear] range
	 */
	@Override
	public void install(Object o) {
		
		LOGGER.info("Implementing vehicle electrification.");
		
		DemandModel dm = null;
		if (o instanceof DemandModel) {
			dm = (DemandModel)o;
		}
		else {
			LOGGER.error("VehicleElectrification installation has received an unexpected type.");
			return;
		}
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));
		VehicleType vht = VehicleType.valueOf(props.getProperty("vehicleType"));

		//set fractions for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++) {
			HashMap<RoadNetworkAssignment.EngineType, Double> engineTypeFractions = new HashMap<RoadNetworkAssignment.EngineType, Double>();
			//read fractions from the properties
			for (RoadNetworkAssignment.EngineType et: RoadNetworkAssignment.EngineType.values()) {
				double fraction = Double.parseDouble(this.props.getProperty(et.name()));
				engineTypeFractions.put(et, fraction);
			}
			
			dm.setEngineTypeFractions(y, vht, engineTypeFractions);
		}
		
		this.installed = true;
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#uninstall(java.lang.Object)
	 * Installs default (base-year) engine fractions for all years in the [startYear, endYear] range
	 */
	@Override
	public void uninstall(Object o) {
		
		LOGGER.info("Removing vehicle electrification.");
		
		DemandModel dm = null;
		if (o instanceof DemandModel) {
			dm = (DemandModel)o;
		}
		else {
			LOGGER.error("VehicleElectrification uninstallation has received an unexpected type.");
			return;
		}
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		int endYear = Integer.parseInt(props.getProperty("endYear"));
		VehicleType vht = VehicleType.valueOf(props.getProperty("vehicleType"));
		
		//set base-year fractions for all years from startYear to endYear
		for (int y = startYear; y <= endYear; y++) {
			HashMap<RoadNetworkAssignment.EngineType, Double> engineTypeFractions = new HashMap<RoadNetworkAssignment.EngineType, Double>();
			//read fractions from the base-year fractions
			for (RoadNetworkAssignment.EngineType et: RoadNetworkAssignment.EngineType.values()) {
				double fraction = dm.getEngineTypeFractions(DemandModel.BASE_YEAR).get(vht).get(et);
				engineTypeFractions.put(et, fraction);
			}
			dm.setEngineTypeFractions(y, vht, engineTypeFractions);
		}
		
		this.installed = false;
	}
	
	/**
	 * Gets engine fractions as a pie dataset to be used for pie charting.
	 * @return Pie dataset with engine fractions.
	 */
	public DefaultPieDataset getPieDataSet() {
	
		DefaultPieDataset pieDataset = new DefaultPieDataset();
				
		for (RoadNetworkAssignment.EngineType et: RoadNetworkAssignment.EngineType.values()) {
			double fraction = Double.parseDouble(this.props.getProperty(et.name()));
			pieDataset.setValue(et.name(), fraction);
		}
		
		return pieDataset;
	}
}
