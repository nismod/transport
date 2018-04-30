package nismod.transport.utility;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.demand.DemandModel.ElasticityTypes;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * InputFileReader reads input files and provides them as various data structures required by other classes.
 * @author Milan Lovric
 *
 */
public class InputFileReader {
	
	private final static Logger LOGGER = LogManager.getLogger(InputFileReader.class);

	public InputFileReader() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Reads population file.
	 * @param fileName File name.
	 * @return Map with population data.
	 */
	public static HashMap<Integer, HashMap<String, Integer>> readPopulationFile (String fileName) {

		HashMap<Integer, HashMap<String, Integer>> map = new HashMap<Integer, HashMap<String, Integer>>();

		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			int population;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				HashMap<String, Integer> zoneToPopulation = new HashMap<String, Integer>();
				for (String zone: keySet) {
					//System.out.println("Destination zone = " + destination);
					population = Integer.parseInt(record.get(zone));
					zoneToPopulation.put(zone, population);			
				}
				map.put(year, zoneToPopulation);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
		
		LOGGER.debug("Population:");
		LOGGER.debug(map);

		return map;
	}

	/**
	 * Reads GVA file.
	 * @param fileName File name.
	 * @return Map with GVA data.
	 */
	public static HashMap<Integer, HashMap<String, Double>> readGVAFile (String fileName) {

		HashMap<Integer, HashMap<String, Double>> map = new HashMap<Integer, HashMap<String, Double>>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			//System.out.println("keySet = " + keySet);
			double GVA;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				HashMap<String, Double> zoneToGVA = new HashMap<String, Double>();
				for (String zone: keySet) {
					//System.out.println("Destination zone = " + destination);
					GVA = Double.parseDouble(record.get(zone));
					zoneToGVA.put(zone, GVA);			
				}
				map.put(year, zoneToGVA);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("GVA:");
		LOGGER.debug(map);
		
		return map;
	}
	
	/**
	 * Reads elasticities file.
	 * @param fileName File name.
	 * @return Map with elasticity parameters.
	 */
	public static HashMap<ElasticityTypes, Double> readElasticitiesFile (String fileName) {

		HashMap<ElasticityTypes, Double> map = new HashMap<ElasticityTypes, Double>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				ElasticityTypes et = ElasticityTypes.valueOf(record.get(0));
				Double elasticity = Double.parseDouble(record.get(1));		
				map.put(et, elasticity);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
		
		LOGGER.debug("Elasticities:");
		LOGGER.debug(map);

		return map;
	}
	
	/**
	 * Reads vehicle type to PCU conversion file.
	 * @param fileName File name.
	 * @return Map with PCU equivalents.
	 */
	public static HashMap<VehicleType, Double> readVehicleTypeToPCUFile (String fileName) {

		HashMap<VehicleType, Double> map = new HashMap<VehicleType, Double>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				VehicleType vt = VehicleType.valueOf(record.get(0));
				Double pcu = Double.parseDouble(record.get(1));		
				map.put(vt, pcu);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
		
		LOGGER.debug("Vehicle types to PCU:");
		LOGGER.debug(map);

		return map;
	}
	
	/**
	 * Reads time of day distribution file file.
	 * @param fileName File name.
	 * @return Time of day distribution.
	 */
	public static HashMap<TimeOfDay, Double> readTimeOfDayDistributionFile (String fileName) {

		HashMap<TimeOfDay, Double> map = new HashMap<TimeOfDay, Double>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				TimeOfDay hour = TimeOfDay.valueOf(record.get(0));
				Double frequency = Double.parseDouble(record.get(1));		
				map.put(hour, frequency);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
		
		LOGGER.debug("Time of day distribution:");
		LOGGER.debug(map);

		return map;
	}
	
	/**
	 * Reads energy unit costs file.
	 * @param fileName File name.
	 * @return Map with energy unit costs.
	 */
	public static HashMap<Integer, HashMap<EnergyType, Double>> readEnergyUnitCostsFile (String fileName) {

		HashMap<Integer, HashMap<EnergyType, Double>> map = new HashMap<Integer, HashMap<EnergyType, Double>>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			//System.out.println("keySet = " + keySet);
			double unitPrice;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				HashMap<EnergyType, Double> energyTypeToPrice = new HashMap<EnergyType, Double>();
				for (String et: keySet) {
					//System.out.println("Destination zone = " + destination);
					EnergyType energyType = EnergyType.valueOf(et);
					unitPrice = Double.parseDouble(record.get(energyType));
					energyTypeToPrice.put(energyType, unitPrice);			
				}
				map.put(year, energyTypeToPrice);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Energy unit costs:");
		LOGGER.debug(map);

		return map;
	}
	
	/**
	 * Reads engine type fractions file.
	 * @param fileName File name.
	 * @return Map with engine type fractions.
	 */
	public static HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> readEnergyConsumptionParamsFile (String fileName){
		
		HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> map = new HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>>();

		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("EngineType");
			keySet.remove("VehicleType");
			//remaining keys are "A", "B", "C", "D"
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				VehicleType vt = VehicleType.valueOf(record.get(0));
				EngineType et = EngineType.valueOf(record.get(1));
				
				HashMap<String, Double> parameters = new HashMap<String, Double>();
				for (String key: keySet)
					parameters.put(key, Double.valueOf(record.get(key)));
				map.put(Pair.of(vt, et), parameters);
			}
 
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Energy consumption parameters:");
		LOGGER.debug(map);
		
		return map;
	}
		
	/**
	 * Reads engine type fractions file.
	 * @param fileName File name.
	 * @return Map with engine type fractions.
	 */
	public static HashMap<Integer, HashMap<VehicleType, HashMap<EngineType, Double>>> readEngineTypeFractionsFile (String fileName){
		
		HashMap<Integer, HashMap<VehicleType, HashMap<EngineType, Double>>> yearToVehicleToEngineTypeFractions = new HashMap<Integer, HashMap<VehicleType, HashMap<EngineType, Double>>>();

		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			keySet.remove("vehicle");
			//System.out.println("keySet = " + keySet);
			double fraction;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));

				HashMap<VehicleType, HashMap<EngineType, Double>> vehicleToEngineTypeFractions = yearToVehicleToEngineTypeFractions.get(year);
				if (vehicleToEngineTypeFractions == null) { vehicleToEngineTypeFractions = new HashMap<VehicleType, HashMap<EngineType, Double>>();
				yearToVehicleToEngineTypeFractions.put(year, vehicleToEngineTypeFractions);
				}

				VehicleType vht = VehicleType.valueOf(record.get(1));
				HashMap<EngineType, Double> engineTypeFractions = vehicleToEngineTypeFractions.get(vht);
				if (engineTypeFractions == null) { engineTypeFractions = new HashMap<EngineType, Double>();
				vehicleToEngineTypeFractions.put(vht, engineTypeFractions);
				}

				for (String et: keySet) {
					//System.out.println("Destination zone = " + destination);
					EngineType engineType = EngineType.valueOf(et);
					fraction = Double.parseDouble(record.get(engineType));
					engineTypeFractions.put(engineType, fraction);			
				}
			} 
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Year to vehicle engine type fractions:");
		LOGGER.debug(yearToVehicleToEngineTypeFractions);
		
		return yearToVehicleToEngineTypeFractions;
	}
	
	/**
	 * Reads autonomous vehicles fractions file.
	 * @param fileName File name.
	 * @return Map with predictions of autonomous vehicles fractions.
	 */
	public static HashMap<Integer, Double> readAVFractionsFile(String fileName) {

		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				double fraction = Double.parseDouble(record.get(1));		
				map.put(year, fraction);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
		
		LOGGER.debug("Autonomous vehicle fractions:");
		LOGGER.debug(map);

		return map;
	}
}
