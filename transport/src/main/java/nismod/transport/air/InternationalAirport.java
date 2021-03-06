package nismod.transport.air;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class stores information about an international (non-UK) airport. 
 * @author Milan Lovric
 *
 */
public class InternationalAirport extends Airport {
	
	private final static Logger LOGGER = LogManager.getLogger(InternationalAirport.class);
	
	/**
	 * Constructor for the airport.
	 * @param iataCode Airport IATA code.
	 * @param caaName Airport name in CCA datasets.
	 * @param ourAirportsName Airport name in ourAirports dataset.
	 * @param longitude Longitude coordinate.
	 * @param latitude Latitude coordinate.
	 * @param countryCode Code of the country in which the airport is located.
	 * @param countryCode Code of the continent in which the airport is located.
	 * @param terminalCapacity Airport terminal capacity (max number of passengers that can be processed).
	 * @param runwayCapacity Airport runway capacity (max number of flights that can be processed).
	 */
	public InternationalAirport(String iataCode, String caaName, String ourAirportsName, double longitude, double latitude,
						  String countryCode, String continentCode, long terminalCapacity, long runwayCapacity) {
				
		super(iataCode, caaName, ourAirportsName, longitude, latitude, countryCode, continentCode, terminalCapacity, runwayCapacity);
	}
	
	/**
	 * Constructor for an airport using an existing airport.
	 * @param airport Airport which data is going to be copied.
	 */
	public InternationalAirport(InternationalAirport airport) {
		
		super(airport.getIataCode(), airport.getCAAName(), airport.getOurAirportsName(),
			 airport.getLongitude(), airport.getLatitude(), airport.getCountry().getCountry(),
			 airport.getContinent().toString(), airport.getTerminalCapacity(), airport.getRunwayCapacity());
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.getIataCode());
		sb.append(", ");
		sb.append(this.getCAAName());
		sb.append(", ");
		sb.append(this.getOurAirportsName());
		sb.append(", ");
		sb.append(this.getLongitude());
		sb.append(", ");
		sb.append(this.getLatitude());
		sb.append(", ");
		sb.append(this.getCountry().getCountry());
		sb.append(", ");
		sb.append(this.getCountry().getDisplayName());
		sb.append(", ");
		sb.append(this.getContinent());
		sb.append(", ");
		sb.append(this.getContinent().getName());
		sb.append(", ");
		sb.append(this.getTerminalCapacity());
		sb.append(", ");
		sb.append(this.getRunwayCapacity());
		return sb.toString();
	}
}
