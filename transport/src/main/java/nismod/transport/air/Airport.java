package nismod.transport.air;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class stores information about an airport. 
 * @author Milan Lovric
 *
 */
public abstract class Airport {
	
	private final static Logger LOGGER = LogManager.getLogger(Airport.class);
	
	private String iataCode; // three letter code
	private String caaName; //airport name in CAA dataset
	private String ourAirportsName; //longer name (from ourAirports dataset)
	private double longitude;
	private double latitude;
	private Locale country;
	private AirportGroupCAA airportGroupCAA;
	private ContinentCode continent;
	private long terminalCapacity;
	private long runwayCapacity;
	
	/**
	 * Airport groups by CAA used in the flight movements dataset.
	 */
	public static enum AirportGroupCAA {
		UK, //United Kingdom
		EU, //International (EU)
		INT //International (non-EU)
	}
	
	/**
	 * Airports grouped by DfT.
	 */
	public static enum AirportGroup {
		DO, //Domestic
		SH, //Short-haul (Western Europe + Turkey)
		LH //Long-haul
	}
	
	/**
	 * ISO continent code
	 */
	public static enum ContinentCode {
		OC ("Oceania"),
		SA ("South America"),
		AS ("Asia"),
		EU ("Europe"),
		AF ("Africa"),
		NA ("North America"),
		AN ("Antartica");
		
	    private final String name;

	    /**
	     * @param name
	     */
	    private ContinentCode (final String name) {
	        this.name = name;
	    }

	    /**
	     * @return
	     */
	    public String getName() {
	        return name;
	    }
	}
	
	/**
	 * These are the foreign region groups used by CAA for international internodal passenger demand.
	 * There is 1:1 mapping between a country and a region (this is unlike the OurAirports data where one country
	 * could map to multiple regions, e.g. some Russian airports are in Asia, while some are in Europe).
	 */
	public static enum ForeignRegionCAA {
		
		ATLANTIC_OCEAN_ISLANDS ("ATLANTIC OCEAN ISLANDS"),
		AUSTRALASIA ("AUSTRALASIA"),
		CANADA ("CANADA"),
		CARRIBEAN_AREA ("CARIBBEAN AREA"),
		CENTRAL_AFRICA ("CENTRAL AFRICA"),
		CENTRAL_AMERICA ("CENTRAL AMERICA"),
		EAST_AFRICA ("EAST AFRICA"),
		EASTERN_EUROPE_OTHER ("EASTERN EUROPE-OTHER"),
		EASTERN_EUROPE_EU ("EASTERN EUROPE-EU"),
		FAR_EAST ("FAR EAST"),
		INDIAN_OCEAN_ISLANDS ("INDIAN OCEAN ISLANDS"),
		INDIAN_SUB_CONTINENT ("INDIAN SUB-CONTINENT"),
		MIDDLE_EAST ("MIDDLE EAST"),
		NEAR_EAST ("NEAR EAST"),
		NORTH_AFRICA ("NORTH AFRICA"),
		OIL_RIGS ("OIL RIGS"),
		PACIFIC_OCEAN_ISLANDS ("PACIFIC OCEAN ISLANDS"),
		SOUTH_AMERICA ("SOUTH AMERICA"),
		SOUTHERN_AFRICA ("SOUTHERN AFRICA"),
		UNITED_STATES_OF_AMERICA ("UNITED STATES OF AMERICA"),
		WEST_AFRICA ("WEST AFRICA"),
		WESTERN_EUROPE_EU ("WESTERN EUROPE-EU"),
		WESTERN_EUROPE_OTHER ("WESTERN-EUROPE-OTHER");
		
	    private final String name;

	    /**
	     * @param name
	     */
	    private ForeignRegionCAA (final String name) {
	        this.name = name;
	    }

	    /**
	     * @return
	     */
	    public String getName() {
	        return name;
	    }
	} 
	
	/**
	 * Constructor for the airport.
	 * @param iataCode Airport IATA code.
	 * @param caaName Airport name in CAA datasets.
	 * @param ourAirportsName Airport name in ourAirports dataset.
	 * @param longitude Longitude coordinate.
	 * @param latitude Latitude coordinate.
	 * @param countryCode Code of the country in which the airport is located.
	 * @param continentCode Code of the continent in which the airport is located.
	 * @param terminalCapacity Airport terminal capacity (max number of passengers that can be processed).
	 * @param runwayCapacity Airport runway capacity (max number of flights that can be processed).
	 */
	public Airport(String iataCode, String caaName, String ourAirportsName, double longitude, double latitude, String countryCode, String continentCode, long terminalCapacity, long runwayCapacity) {
		
		this.iataCode = iataCode;
		this.caaName = caaName;
		this.ourAirportsName = ourAirportsName;
		this.longitude = longitude;
		this.latitude = latitude;
		this.country = new Locale("", countryCode);
		this.continent = ContinentCode.valueOf(continentCode);
		this.terminalCapacity = terminalCapacity;
		this.runwayCapacity = runwayCapacity;
	}
	
	/**
	 * Constructor for an airport.
	 * @param airport Airport which data is going to be copied.
	 */
	public Airport(Airport airport) {
		
		this(airport.getIataCode(), airport.getCAAName(), airport.getOurAirportsName(),
			 airport.getLongitude(), airport.getLatitude(), airport.getCountry().getISO3Country(),
			 airport.getContinent().toString(), airport.getTerminalCapacity(), airport.getRunwayCapacity());
	}
	
	/**
	 * Getter method for the IATA code of the station.
	 * @return IATA code.
	 */
	public String getIataCode() {
		
		return this.iataCode;
	}
	
	/**
	 * Getter method for the airport CAA name.
	 * @return CAA name.
	 */
	public String getCAAName() {
		
		return this.caaName;
	}
	
	/**
	 * Getter method for the ourAirports name.
	 * @return OurAirports name.
	 */
	public String getOurAirportsName() {
		
		return this.ourAirportsName;
	}
	
	/**
	 * Getter method for longitude.
	 * @return Longitude.
	 */
	public double getLongitude() {
		
		return this.longitude;
	}
	
	/**
	 * Getter method for Latitude.
	 * @return Latitude.
	 */
	public double getLatitude() {
		
		return this.latitude;
	}
	
	/**
	 * Getter method for the country in which the airport is located.
	 * @return Country locale.
	 */
	public Locale getCountry() {
		
		return this.country;
	}
	
	/**
	 * Getter method for the continent in which the airport is located.
	 * @return ContinentCode continent.
	 */
	public ContinentCode getContinent() {
		
		return this.continent;
	}
	
	/**
	 * Getter method for the airport terminal capacity.
	 * @return Terminal capacity.
	 */
	public long getTerminalCapacity() {
		
		return this.terminalCapacity;
	}
	
	/**
	 * Getter method for the airport runway capacity.
	 * @return Runway capacity.
	 */
	public long getRunwayCapacity() {
		
		return this.runwayCapacity;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.iataCode);
		sb.append(", ");
		sb.append(this.caaName);
		sb.append(", ");
		sb.append(this.ourAirportsName);
		sb.append(", ");
		sb.append(this.longitude);
		sb.append(", ");
		sb.append(this.latitude);
		sb.append(", ");
		sb.append(this.country.getCountry());
		sb.append(", ");
		sb.append(this.country.getDisplayName());
		sb.append(", ");
		sb.append(this.continent);
		sb.append(", ");
		sb.append(this.continent.getName());
		sb.append(", ");
		sb.append(this.terminalCapacity);
		sb.append(", ");
		sb.append(this.runwayCapacity);

		return sb.toString();
	}

}
