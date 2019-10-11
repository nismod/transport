package nismod.transport.air;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class encapsulates internodal (airport to airport) passenger data.
 * @author Milan Lovric
 */
public abstract class InternodalPassengerDemand {

	private final static Logger LOGGER = LogManager.getLogger(InternodalPassengerDemand.class);

	//private MultiKeyMap<MultiKey<String>, Map<Passengers, Long>> data;
	protected MultiKeyMap data;

	public static enum Passengers {
		TOTAL,
		SCHEDULED,
		CHARTER
	}

	public InternodalPassengerDemand() {

		data = new MultiKeyMap();
	}
	
	public void printDemand() {
		
		for (Object entry: data.entrySet()) {
		//	Map<Passengers, Long> map = (Map<Passengers, Long>) value; 
			System.out.println(entry);
		}
	}
	
	public Map<Passengers, Long> getDemand(String firstIATA, String secondIATA) {
	
		return (Map<Passengers, Long>) this.data.get(firstIATA, secondIATA);
	}
	
	public void setDemand(String firstIATA, String secondIATA, long totalPax, long scheduledPax, long charterPax) {
		
		Map<Passengers, Long> map = (Map<Passengers, Long>) this.data.get(firstIATA, secondIATA);
		if (map == null) {
				map = new EnumMap<>(Passengers.class);
				this.data.put(firstIATA, secondIATA, map);
		}

		map.put(Passengers.TOTAL, totalPax);
		map.put(Passengers.SCHEDULED, scheduledPax);
		map.put(Passengers.CHARTER, charterPax);
	}
	
	public abstract void saveAirPassengerDemand(int year, String file);
}