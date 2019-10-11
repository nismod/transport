package nismod.transport.air;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class stores information about a domestic (UK) airport. 
 * @author Milan Lovric
 *
 */
public class DomesticAirport extends Airport {
	
	private final static Logger LOGGER = LogManager.getLogger(DomesticAirport.class);
	
	private String atcoCode;
	private String naptanName; //longer NaPTAN name (for UK airports only)
	private int easting; //for UK airports
	private int northing; //for UK airports
	private String ladCode; //for UK airports
	private String ladName; //for UK airports
	
	/**
	 * Constructor for the airport.
	 * @param iataCode Airport IATA code.
	 * @param atcoCode Airport ATCO code.
	 * @param caaName Airport name in CCA datasets.
	 * @param naptanName Airport name in NaPTAN (for UK airpots).
	 * @param ourAirportsName Airport name in ourAirports dataset.
	 * @param easting Easting coordinate.
	 * @param northing Northing coordinate.
	 * @param longitude Longitude coordinate.
	 * @param latitude Latitude coordinate.
	 * @param ladCode LAD code of the zone in which the airport is located (for UK airports).
	 * @param ladName LAD name of the zone in which the airport is located (for UK airports).
	 * @param terminalCapacity Airport terminal capacity (max number of passengers that can be processed).
	 * @param runwayCapacity Airport runway capacity (max number of flights that can be processed).
	 */
	public DomesticAirport(String iataCode, String atcoCode, String caaName, String naptanName, String ourAirportsName, int easting, int northing, 
				   double longitude, double latitude, String ladCode, String ladName, long terminalCapacity, long runwayCapacity) {
		
		
		super(iataCode, caaName, ourAirportsName, longitude, latitude, "GB", "EU", terminalCapacity, runwayCapacity);
		
		this.atcoCode = atcoCode;
		this.naptanName = naptanName;
		this.easting = easting;
		this.northing = northing;
		this.ladCode = ladCode;
		this.ladName = ladName;
	}
	
	/**
	 * Constructor for an airport.
	 * @param airport Airport which data is going to be copied.
	 */
	public DomesticAirport(DomesticAirport airport) {
		
		this(airport.getIataCode(), airport.getAtcoCode(), airport.getCAAName(), airport.getNaPTANName(), airport.getOurAirportsName(),
			 airport.getEasting(), airport.getNorthing(), airport.getLongitude(), airport.getLatitude(), airport.getLADCode(),
			 airport.getLADName(), airport.getTerminalCapacity(), airport.getRunwayCapacity());
	}
	
	/**
	 * Getter method for the airport ATCO code.
	 * @return NaPTAN name.
	 */
	public String getAtcoCode() {
		
		return this.atcoCode;
	}
	
	/**
	 * Getter method for the airport NaPTAN name.
	 * @return NaPTAN name.
	 */
	public String getNaPTANName() {
		
		return this.naptanName;
	}
	
	/**
	 * Getter method for easting.
	 * @return Easting.
	 */
	public int getEasting() {
		
		return this.easting;
	}
	
	/**
	 * Getter method for Northing.
	 * @return Northing.
	 */
	public int getNorthing() {
		
		return this.northing;
	}
	
	/**
	 * Getter method for the LAD code in which station is located.
	 * @return LAD code.
	 */
	public String getLADCode() {
		
		return this.ladCode;
	}
	
	/**
	 * Getter method for the LAD name in which station is located.
	 * @return LAD name.
	 */
	public String getLADName() {
		
		return this.ladName;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.getIataCode());
		sb.append(", ");
		sb.append(this.getAtcoCode());
		sb.append(", ");
		sb.append(this.getCAAName());
		sb.append(", ");
		sb.append(this.naptanName);
		sb.append(", ");
		sb.append(this.getOurAirportsName());
		sb.append(", ");
		sb.append(this.easting);
		sb.append(", ");
		sb.append(this.northing);
		sb.append(", ");
		sb.append(this.getLongitude());
		sb.append(", ");
		sb.append(this.getLatitude());
		sb.append(", ");
		sb.append(this.ladCode);
		sb.append(", ");
		sb.append(this.ladName);
		sb.append(", ");
		sb.append(this.getCountry().getCountry());
		sb.append(", ");
		sb.append(this.getCountry().getDisplayName());
		sb.append(", ");
		sb.append(this.getContinent().toString());
		sb.append(", ");
		sb.append(this.getContinent().getName());
		sb.append(", ");
		sb.append(this.getTerminalCapacity());
		sb.append(", ");
		sb.append(this.getRunwayCapacity());
		return sb.toString();
	}

}
