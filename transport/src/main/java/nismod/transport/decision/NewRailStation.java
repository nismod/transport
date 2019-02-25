/**
 * 
 */
package nismod.transport.decision;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.rail.RailDemandModel;
import nismod.transport.rail.RailDemandModel.ElasticityArea;
import nismod.transport.rail.RailStation;
import nismod.transport.rail.RailStation.RailModeType;
import nismod.transport.rail.RailStationDemand;

/**
 * Intervention that builds a new rail station.
 * @author Milan Lovric
 *
 */
public class NewRailStation extends Intervention {
	
	private final static Logger LOGGER = LogManager.getLogger(NewRailStation.class);
	
	private Integer nlc = null;
	
	/** Constructor.
	 * @param props Properties of the intervention.
	 */
	public NewRailStation (Properties props) {
		
		super(props);
	}
	
	/** Constructor.
	 * @param fileName File with the properties.
	 */
	public NewRailStation (String fileName) {
		
		super(fileName);
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#install(java.lang.Object)
	 */
	@Override
	public void install(Object o) {
		
		LOGGER.info("Implementing new rail station.");
		
		RailDemandModel rdm = null;
		if (o instanceof RailDemandModel) {
			rdm = (RailDemandModel) o;
		} else {
			LOGGER.error("NewRailStation installation has received an unexpected type.");
			return;
		}

		int startYear = this.getStartYear();
		int endYear = this.getEndYear();
				
		int nlc = Integer.parseInt(this.props.getProperty("NLC"));
		RailModeType mode = RailModeType.valueOf(this.props.getProperty("mode"));
		String stationName = this.props.getProperty("station");
		String naptanName = this.props.getProperty("naPTANname");
		int easting = Integer.parseInt(this.props.getProperty("easting"));
		int northing = Integer.parseInt(this.props.getProperty("northing"));
		int yearUsage = Integer.parseInt(this.props.getProperty("yearUsage"));
		double dayUsage = Double.parseDouble(this.props.getProperty("dayUsage"));
		int runDays = Integer.parseInt(this.props.getProperty("runDays"));
		String ladCode = this.props.getProperty("LADcode");
		String ladName = this.props.getProperty("LADname");
		ElasticityArea area = ElasticityArea.valueOf(this.props.getProperty("area"));
		
		//if not already installed
		if (!this.getState()) {
			
			int baseYear = RailDemandModel.baseYear;
			List<String> header = rdm.getRailStationDemand(baseYear).getHeader();
		
			//add station for all years between startYear and endYear
			for (int year = startYear; year <= endYear; year++) {
				
				//fetch rail demand for year i
				RailStationDemand rsd = rdm.getRailStationDemand(year);
				if (rsd == null) {
					rsd = new RailStationDemand(header);
					rdm.setRailStationDemand(year, rsd);
				}
		
				//create new station
				RailStation station = new RailStation(nlc, mode, stationName, naptanName, easting, northing,
												yearUsage, dayUsage, runDays, ladCode, ladName, area);
				//add station to demand
				rsd.addStation(station);
			}
		
			this.nlc = nlc;
			this.installed = true;
		}
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#uninstall(nismod.transport.demand.DemandModel)
	 */
	@Override
	public void uninstall(Object o) {
		
		LOGGER.warn("Cannot uninstall rail station.");
	}
	
	/**
	 * @return NLC code of the new rail station.
	 */
	public Integer getNLC() {
		
		if (this.nlc == null) {
			LOGGER.warn("Unknown NLC of the new rail station!");
			return null;
		}
		return this.nlc;
	}
}
