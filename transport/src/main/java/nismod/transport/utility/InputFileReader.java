package nismod.transport.utility;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.air.AirDemandModel;
import nismod.transport.air.Airport;
import nismod.transport.air.DomesticAirport;
import nismod.transport.air.InternationalAirport;
import nismod.transport.demand.DemandModel.ElasticityTypes;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.rail.RailDemandModel;
import nismod.transport.rail.RailDemandModel.ElasticityArea;

/**
 * InputFileReader reads input files and provides them as various data structures required by other classes.
 * @author Milan Lovric
 */
public class InputFileReader {

	private final static Logger LOGGER = LogManager.getLogger();

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
		int zonesNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			zonesNumber = keySet.size();
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

		LOGGER.debug("Population file read with data values for {} years and {} zones.", map.keySet().size(), zonesNumber);

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
		int zonesNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			zonesNumber = keySet.size();
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

		LOGGER.debug("GVA file read with data values for {} years and {} zones.", map.keySet().size(), zonesNumber);

		return map;
	}

	/**
	 * Reads elasticities file.
	 * @param fileName File name.
	 * @return Map with elasticity parameters.
	 */
	public static Map<ElasticityTypes, Double> readElasticitiesFile (String fileName) {

		Map<ElasticityTypes, Double> map = new EnumMap<>(ElasticityTypes.class);
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

		LOGGER.debug("Road elasticities read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads rail elasticities file.
	 * @param fileName File name.
	 * @return Map with elasticity parameters.
	 */
	public static Map<RailDemandModel.ElasticityTypes, Map<RailDemandModel.ElasticityArea, Double>> readRailElasticitiesFile (String fileName) {

		Map<RailDemandModel.ElasticityTypes, Map<ElasticityArea, Double>> map = new EnumMap<>(RailDemandModel.ElasticityTypes.class);
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				RailDemandModel.ElasticityTypes et = RailDemandModel.ElasticityTypes.valueOf(record.get(0));
				ElasticityArea ea = ElasticityArea.valueOf(record.get(1));
				Double elasticity = Double.parseDouble(record.get(2));

				//fetch map
				Map<ElasticityArea, Double> fm = map.get(et);
				if (fm == null) {
					fm = new EnumMap<>(ElasticityArea.class);
					map.put(et,  fm);
				}
				fm.put(ea, elasticity);
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

		LOGGER.debug("Rail elasticities read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads air elasticities file.
	 * @param fileName File name.
	 * @return Map with elasticity parameters.
	 */
	public static Map<AirDemandModel.ElasticityTypes, Double> readAirElasticitiesFile (String fileName) {

		Map<AirDemandModel.ElasticityTypes, Double> map = new EnumMap<>(AirDemandModel.ElasticityTypes.class);
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				AirDemandModel.ElasticityTypes et = AirDemandModel.ElasticityTypes.valueOf(record.get(0));
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

		LOGGER.debug("Air elasticities read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads vehicle type to PCU conversion file.
	 * @param fileName File name.
	 * @return Map with PCU equivalents.
	 */
	public static Map<VehicleType, Double> readVehicleTypeToPCUFile (String fileName) {

		Map<VehicleType, Double> map = new EnumMap<>(VehicleType.class);
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

		LOGGER.debug("Vehicle types to PCU read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads time of day distribution file for passenger car vehicles.
	 * @param fileName File name.
	 * @return Time of day distribution.
	 */
	public static Map<Integer, Map<TimeOfDay, Double>> readTimeOfDayDistributionFile (String fileName) {

		Map<Integer, Map<TimeOfDay, Double>> map = new HashMap<Integer, Map<TimeOfDay, Double>>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				Map<TimeOfDay, Double> timeDistribution = new EnumMap<>(TimeOfDay.class);

				for (String key: keySet) {
					TimeOfDay hour = TimeOfDay.valueOf(key);
					Double frequency = Double.parseDouble(record.get(key));
					timeDistribution.put(hour, frequency);
				}

				map.put(year, timeDistribution);
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

		LOGGER.debug("Time of day distribution read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads time of day distribution file for freight vehicles.
	 * @param fileName File name.
	 * @return Time of day distribution.
	 */
	public static Map<Integer, Map<VehicleType, Map<TimeOfDay, Double>>> readTimeOfDayDistributionFreightFile (String fileName) {

		Map<Integer, Map<VehicleType, Map<TimeOfDay, Double>>> map = new HashMap<Integer, Map<VehicleType, Map<TimeOfDay, Double>>>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			keySet.remove("vehicle");
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));

				Map<VehicleType, Map<TimeOfDay, Double>> vehicleToTimeOfDayFractions = map.get(year);
				if (vehicleToTimeOfDayFractions == null) {
					vehicleToTimeOfDayFractions = new EnumMap<>(VehicleType.class);
					map.put(year, vehicleToTimeOfDayFractions);
				}

				VehicleType vht = VehicleType.valueOf(record.get(1));
				Map<TimeOfDay, Double> timeDistribution = vehicleToTimeOfDayFractions.get(vht);
				if (timeDistribution == null) {
					timeDistribution = new EnumMap<>(TimeOfDay.class);
					vehicleToTimeOfDayFractions.put(vht, timeDistribution);
				}

				for (String time: keySet) {
					TimeOfDay hour = TimeOfDay.valueOf(time);
					Double frequency = Double.parseDouble(record.get(time));
					timeDistribution.put(hour, frequency);
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

		LOGGER.debug("Time of day freight distribution read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads energy unit costs file.
	 * @param fileName File name.
	 * @return Map with energy unit costs.
	 */
	public static HashMap<Integer, Map<EnergyType, Double>> readEnergyUnitCostsFile (String fileName) {

		HashMap<Integer, Map<EnergyType, Double>> map = new HashMap<Integer, Map<EnergyType, Double>>();
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
				Map<EnergyType, Double> energyTypeToPrice = new EnumMap<>(EnergyType.class);
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

		LOGGER.debug("Energy unit costs read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads unit CO2 emissions file.
	 * @param fileName File name.
	 * @return Map with unit CO2 emissions.
	 */
	public static HashMap<Integer, Map<EnergyType, Double>> readUnitCO2EmissionFile (String fileName) {

		HashMap<Integer, Map<EnergyType, Double>> map = new HashMap<Integer, Map<EnergyType, Double>>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			//System.out.println("keySet = " + keySet);
			double unitEmission;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				Map<EnergyType, Double> energyTypeToUnitCO2Emission = new EnumMap<>(EnergyType.class);
				for (String et: keySet) {
					//System.out.println("Destination zone = " + destination);
					EnergyType energyType = EnergyType.valueOf(et);
					unitEmission = Double.parseDouble(record.get(energyType));
					energyTypeToUnitCO2Emission.put(energyType, unitEmission);
				}
				map.put(year, energyTypeToUnitCO2Emission);
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

		LOGGER.debug("Unit CO2 emissions read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads engine type fractions file.
	 * @param fileName File name.
	 * @return Map with engine type fractions.
	 */
	public static Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> readEnergyConsumptionParamsFile (String fileName){

		Map<VehicleType, Map<EngineType, Map<WebTAG, Double>>> map = new EnumMap<VehicleType, Map<EngineType, Map<WebTAG, Double>>>(VehicleType.class);

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

				Map<WebTAG, Double> parameters = new EnumMap<>(WebTAG.class);
				for (String key: keySet) {
					WebTAG pt = WebTAG.valueOf(key);
					parameters.put(pt, Double.valueOf(record.get(key)));
				}

				Map<EngineType, Map<WebTAG, Double>> innerMap = map.get(vt);
				if (innerMap == null) {
					innerMap = new EnumMap<EngineType, Map<WebTAG, Double>>(EngineType.class);
					map.put(vt, innerMap);
				}
				map.get(vt).put(et, parameters);
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

		LOGGER.debug("Energy consumption parameters read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads engine type fractions file.
	 * @param fileName File name.
	 * @return Map with engine type fractions.
	 */
	public static HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> readEngineTypeFractionsFile (String fileName){

		HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> yearToVehicleToEngineTypeFractions = new HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>>();

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

				Map<VehicleType, Map<EngineType, Double>> vehicleToEngineTypeFractions = yearToVehicleToEngineTypeFractions.get(year);
				if (vehicleToEngineTypeFractions == null) { vehicleToEngineTypeFractions = new EnumMap<>(VehicleType.class);
				yearToVehicleToEngineTypeFractions.put(year, vehicleToEngineTypeFractions);
				}

				VehicleType vht = VehicleType.valueOf(record.get(1));
				Map<EngineType, Double> engineTypeFractions = vehicleToEngineTypeFractions.get(vht);
				if (engineTypeFractions == null) { engineTypeFractions = new EnumMap<>(EngineType.class);
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

		LOGGER.debug("Year to vehicle engine type fractions file read from file.");
		LOGGER.trace(yearToVehicleToEngineTypeFractions);

		return yearToVehicleToEngineTypeFractions;
	}

	/**
	 * Reads autonomous vehicles fractions file.
	 * @param fileName File name.
	 * @return Map with predictions of autonomous vehicles fractions.
	 */
	public static HashMap<Integer, Map<VehicleType, Double>> readAVFractionsFile(String fileName) {

		HashMap<Integer, Map<VehicleType, Double>>  map = new HashMap<Integer, Map<VehicleType, Double>> ();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				Map<VehicleType, Double> fractionMap = new EnumMap<>(VehicleType.class);
				map.put(year, fractionMap);
				for (String key: keySet) {
					VehicleType vht = VehicleType.valueOf(key);
					double fraction = Double.parseDouble(record.get(key));
					fractionMap.put(vht,  fraction);
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

		LOGGER.debug("Autonomous vehicle fractions read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads relative fuel efficiency file.
	 * @param fileName File name.
	 * @return Map with relative fuel efficiency.
	 */
	public static HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> readRelativeFuelEfficiencyFile (String fileName){

		HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>> yearToRelativeFuelEfficiency = new HashMap<Integer, Map<VehicleType, Map<EngineType, Double>>>();

		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			LOGGER.trace(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("VehicleType");
			keySet.remove("EngineType");
			LOGGER.trace("keySet = {}", keySet);
			double efficiency;
			for (CSVRecord record : parser) {
				LOGGER.trace(record);
				VehicleType vht = VehicleType.valueOf(record.get(0));
				EngineType engine = EngineType.valueOf(record.get(1));

				for(String key: keySet) {
					Integer year = Integer.parseInt(key);
					efficiency = Double.parseDouble(record.get(key));
					Map<VehicleType, Map<EngineType, Double>> vehicleEngineEfficiency = yearToRelativeFuelEfficiency.get(year);
					if (vehicleEngineEfficiency == null) {
						vehicleEngineEfficiency = new EnumMap<VehicleType, Map<EngineType, Double>>(VehicleType.class);
						yearToRelativeFuelEfficiency.put(year, vehicleEngineEfficiency);
					}
					Map<EngineType, Double> innerMap = vehicleEngineEfficiency.get(vht);
					if (innerMap == null) {
						innerMap = new EnumMap<EngineType, Double>(EngineType.class);
						vehicleEngineEfficiency.put(vht, innerMap);
					}
					vehicleEngineEfficiency.get(vht).put(engine, efficiency);
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

		LOGGER.debug("Relative fuel efficiencies read from file.");
		LOGGER.trace(yearToRelativeFuelEfficiency);

		return yearToRelativeFuelEfficiency;
	}

	/**
	 * Reads link travel time file.
	 * @param year Year of the assignment.
	 * @param fileName File name.
	 * @return Link travel time per time of day.
	 */
	public static Map<TimeOfDay, Map<Integer, Double>> readLinkTravelTimeFile(int year, String fileName){

		Map<Integer, Map<TimeOfDay, Map<Integer, Double>>> yearToLinkTravelTimePerTimeOfDay = new HashMap<Integer, Map<TimeOfDay, Map<Integer, Double>>>();

		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			keySet.remove("edgeID");
			keySet.remove("freeFlow");
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int yearFromFile = Integer.parseInt(record.get(0));
				int edgeID = Integer.parseInt(record.get(1));
				//double freeFlow = Double.parseDouble(record.get(2)); //ignore

				Map<TimeOfDay, Map<Integer, Double>> linkTravelTimePerTimeOfDay = yearToLinkTravelTimePerTimeOfDay.get(yearFromFile);
				if (linkTravelTimePerTimeOfDay == null) {
					linkTravelTimePerTimeOfDay = new EnumMap<>(TimeOfDay.class);
					yearToLinkTravelTimePerTimeOfDay.put(yearFromFile, linkTravelTimePerTimeOfDay);
				}
				for (String key: keySet) {
					TimeOfDay hour = TimeOfDay.valueOf(key);
					Double travelTime = Double.parseDouble(record.get(key));

					Map<Integer, Double> linkTravelTimeMap = linkTravelTimePerTimeOfDay.get(hour);
					if (linkTravelTimeMap == null) {
						linkTravelTimeMap = new HashMap<Integer, Double>();
						linkTravelTimePerTimeOfDay.put(hour, linkTravelTimeMap);
					}
					linkTravelTimeMap.put(edgeID, travelTime);
				}//hour
			}//record
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

		LOGGER.debug("Link travel times read from file.");
		LOGGER.trace(yearToLinkTravelTimePerTimeOfDay.get(year));

		return yearToLinkTravelTimePerTimeOfDay.get(year);
	}

	/**
	 * Reads zonal car journey costs file.
	 * @param fileName File name.
	 * @return Map with cost data.
	 */
	public static HashMap<Integer, HashMap<String, Double>> readZonalCarCostsFile(String fileName) {

		HashMap<Integer, HashMap<String, Double>> map = new HashMap<Integer, HashMap<String, Double>>();
		CSVParser parser = null;
		int zonesNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			zonesNumber = keySet.size();
			//System.out.println("keySet = " + keySet);
			double cost;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				HashMap<String, Double> zoneToCost = new HashMap<String, Double>();
				for (String zone: keySet) {
					//System.out.println("Destination zone = " + destination);
					cost = Double.parseDouble(record.get(zone));
					zoneToCost.put(zone, cost);
				}
				map.put(year, zoneToCost);
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

		LOGGER.debug("Zonal car costs read from file with data points for {} years and {} zones.", map.keySet().size(), zonesNumber);
		//LOGGER.debug("Cost:");
		//LOGGER.debug(map);

		return map;
	}

	/**
	 * Reads rail station costs file.
	 * @param fileName File name.
	 * @return Map with rail journey costs.
	 */
	public static HashMap<Integer, HashMap<Integer, Double>> readRailStationCostsFile(String fileName) {

		HashMap<Integer, HashMap<Integer, Double>> map = new HashMap<Integer, HashMap<Integer, Double>>();

		CSVParser parser = null;
		int stationNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			stationNumber = keySet.size();
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				HashMap<Integer, Double> nlcToCost = new HashMap<Integer, Double>();
				for (String nlc: keySet) {
					double cost = Double.parseDouble(record.get(nlc));
					int nlcCode = Integer.parseInt(nlc);
					nlcToCost.put(nlcCode, cost);
				}
				map.put(year, nlcToCost);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Rail station costs read from file with data points for {} years and {} stations.", map.keySet().size(), stationNumber);
		//LOGGER.debug("Cost:");
		//LOGGER.debug(map);

		return map;
	}

	/**
	 * Reads trip rates file.
	 * @param fileName File name.
	 * @return Map with yearly trip rates.
	 */
	public static HashMap<Integer, Double> readTripRatesFile(String fileName) {

		HashMap<Integer, Double> map = new HashMap<Integer, Double>();

		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			for (CSVRecord record : parser) {
				int year = Integer.parseInt(record.get(0));
				double tripRate = Double.parseDouble(record.get(1));
				map.put(year, tripRate);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Trip rates read from file with data points for {} years.", map.keySet().size());

		return map;
	}

	/**
	 * Reads passenger trip rates file (zonal).
	 * @param fileName File name.
	 * @return Map with yearly zonal trip rates.
	 */
	public static HashMap<Integer, HashMap<String, Double>> readPassengerTripRatesFile(String fileName) {

		HashMap<Integer, HashMap<String, Double>> map = new HashMap<Integer, HashMap<String, Double>>();
		CSVParser parser = null;
		int zonesNumber = 0;
		try {
			LOGGER.debug("About to read {}", fileName);
			FileReader filereader = new FileReader(fileName);
			parser = new CSVParser(filereader, CSVFormat.DEFAULT.withHeader());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			zonesNumber = keySet.size();
			LOGGER.debug("Reading {} zones from {}", zonesNumber, fileName);
			double tripRate;
			for (CSVRecord record : parser) {
				int year = Integer.parseInt(record.get(0));
				HashMap<String, Double> zoneToTripRate = new HashMap<String, Double>();
				for (String zone: keySet) {
					tripRate = Double.parseDouble(record.get(zone));
					zoneToTripRate.put(zone, tripRate);
				}
				map.put(year, zoneToTripRate);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				if (parser != null){
					parser.close();
				}
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Trip rates read from file with data points for {} years and {} zones.", map.keySet().size(), zonesNumber);

		return map;
	}

	/**
	 * Reads freight trip rates file.
	 * @param fileName File name.
	 * @return Map with yearly zonal trip rates for freight vehicles.
	 */
	public static HashMap<Integer, Map<VehicleType, HashMap<Integer, Double>>> readFreightTripRatesFile(String fileName) {

		HashMap<Integer, Map<VehicleType, HashMap<Integer, Double>>> map = new HashMap<Integer, Map<VehicleType, HashMap<Integer, Double>>>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			keySet.remove("vehicle");
			double tripRate;
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				VehicleType vht = VehicleType.valueOf(record.get(1));

				//fetch vehicle map for the year
				Map<VehicleType, HashMap<Integer, Double>> vehicleTypeToZonalMap = map.get(year);
				if (vehicleTypeToZonalMap == null) {
					vehicleTypeToZonalMap = new EnumMap<>(VehicleType.class);
					map.put(year, vehicleTypeToZonalMap);
				}

				//fetch zonal map for the vehicle
				HashMap<Integer, Double> zonalMap = vehicleTypeToZonalMap.get(vht);
				if (zonalMap == null) {
					zonalMap = new HashMap<Integer, Double>();
					vehicleTypeToZonalMap.put(vht, zonalMap);
				}

				//read trip rates for all the zones and put in the map
				for (String zone: keySet) {
					Integer zoneID = Integer.parseInt(zone);
					tripRate = Double.parseDouble(record.get(zone));
					zonalMap.put(zoneID, tripRate);
				}
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Freight trip rates read from file with data points for {} years.", map.keySet().size());

		return map;
	}

	/**
	 * Reads airport fare index file.
	 * @param fileName File name.
	 * @return Map with airport fare indices.
	 */
	public static HashMap<Integer, HashMap<String, Double>> readAirportFareIndexFile(String fileName) {

		HashMap<Integer, HashMap<String, Double>> map = new HashMap<Integer, HashMap<String, Double>>();

		CSVParser parser = null;
		int airportNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//System.out.println(parser.getHeaderMap().toString());
			Set<String> keySet = parser.getHeaderMap().keySet();
			keySet.remove("year");
			airportNumber = keySet.size();
			for (CSVRecord record : parser) {
				//System.out.println(record);
				int year = Integer.parseInt(record.get(0));
				HashMap<String, Double> iataToFare = new HashMap<String, Double>();
				for (String iata: keySet) {
					double fare = Double.parseDouble(record.get(iata));
					iataToFare.put(iata, fare);
				}
				map.put(year, iataToFare);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				parser.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		LOGGER.debug("Airport fare index read from file with data points for {} years and {} airports.", map.keySet().size(), airportNumber);

		return map;
	}

	/**
	 * Reads domestic airports file.
	 * @param fileName File name.
	 * @return Mapping between IATA code and airport information.
	 */
	public static Map<String, Airport> readDomesticAirportsFile(String fileName) {

		Map<String, Airport> map = new HashMap<String, Airport>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {

				String iataCode = record.get("IataCode");
				String atcoCode = record.get("AtcoCode");
				String caaName = record.get("CAAname");
				String naptanName = record.get("NaPTANname");
				String ourAirportsName = record.get("OurAirportsName");
				int easting = Integer.parseInt(record.get("Easting"));
				int northing = Integer.parseInt(record.get("Northing"));
				double longitude = Double.parseDouble(record.get("Longitude"));
				double latitude = Double.parseDouble(record.get("Latitude"));
				String ladCode = record.get("LadCode");
				String ladName = record.get("LadName");
				long terminalCapacity = Integer.parseInt(record.get("TerminalCapacity"));
				long runwayCapacity = Integer.parseInt(record.get("RunwayCapacity"));

				//create station object and store into the map
				Airport airport = new DomesticAirport(iataCode, atcoCode, caaName, naptanName, ourAirportsName, easting, northing,
						   longitude, latitude, ladCode, ladName, terminalCapacity, runwayCapacity);

				map.put(iataCode, airport);
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

		LOGGER.debug("Domestic airports read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads international airports file.
	 * @param fileName File name.
	 * @return Mapping between IATA code and airport information.
	 */
	public static Map<String, Airport> readInternationalAirportsFile(String fileName) {

		Map<String, Airport> map = new HashMap<String, Airport>();
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			//Set<String> keySet = parser.getHeaderMap().keySet();
			//System.out.println("keySet = " + keySet);
			for (CSVRecord record : parser) {

				String iataCode = record.get("IataCode");
				String caaName = record.get("CAAname");
				String ourAirportsName = record.get("OurAirportsName");
				double longitude = Double.parseDouble(record.get("Longitude"));
				double latitude = Double.parseDouble(record.get("Latitude"));
				String countryCode = record.get("CountryIsoCode");
				String continentCode = record.get("ContinentCode");
				long terminalCapacity = Integer.parseInt(record.get("TerminalCapacity"));
				long runwayCapacity = Integer.parseInt(record.get("RunwayCapacity"));

				//create station object and store into the map
				Airport airport = new InternationalAirport(iataCode, caaName, ourAirportsName,
						   longitude, latitude, countryCode, continentCode, terminalCapacity, runwayCapacity);

				map.put(iataCode, airport);
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

		LOGGER.debug("Domestic airports read from file.");
		LOGGER.trace(map);

		return map;
	}

	/**
	 * Reads zonal vehicle CO2 emissions file.
	 * @param fileName File name.
	 * @return Map with CO2 emissions data.
	 */
	public static HashMap<Integer, HashMap<VehicleType, HashMap<String, Double>>> readZonalVehicleCO2EmissionsFile(String fileName) {

		HashMap<Integer, HashMap<VehicleType, HashMap<String, Double>>> map = new HashMap<Integer, HashMap<VehicleType, HashMap<String, Double>>>();
		CSVParser parser = null;
		int zonesNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			Set<String> keySet = parser.getHeaderMap().keySet();

			double emission;
			for (CSVRecord record : parser) {
				int year = Integer.parseInt(record.get(0));
				String zone = record.get(1);

				HashMap<VehicleType, HashMap<String, Double>> yearMap = map.get(year);
				if (yearMap == null) {
					yearMap = new HashMap<VehicleType, HashMap<String, Double>>();
					map.put(year, yearMap);
				}

				//for each vehicle
				for (VehicleType vht: VehicleType.values()) {

					HashMap<String, Double> zoneMap = yearMap.get(vht);
					if (zoneMap == null) {
						zoneMap = new HashMap<String, Double>();
						yearMap.put(vht, zoneMap);
					}

					emission = Double.parseDouble(record.get(vht));
					zoneMap.put(zone, emission);
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

		LOGGER.debug("Zonal vehicle emissions read from file with data points for {} years and {} zones.", map.keySet().size(), zonesNumber);

		return map;
	}

	/**
	 * Reads zonal car energy consumptions file.
	 * @param fileName File name.
	 * @return Map with zonal energy consumptions data.
	 */
	public static HashMap<Integer, HashMap<EnergyType, HashMap<String, Double>>> readZonalCarEnergyConsumptionsFile(String fileName) {

		HashMap<Integer, HashMap<EnergyType, HashMap<String, Double>>> map = new HashMap<Integer, HashMap<EnergyType, HashMap<String, Double>>>();
		CSVParser parser = null;
		int zonesNumber = 0;
		try {
			parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
			Set<String> keySet = parser.getHeaderMap().keySet();

			double consumption;
			for (CSVRecord record : parser) {
				int year = Integer.parseInt(record.get(0));
				String zone = record.get(1);

				HashMap<EnergyType, HashMap<String, Double>> yearMap = map.get(year);
				if (yearMap == null) {
					yearMap = new HashMap<EnergyType, HashMap<String, Double>>();
					map.put(year, yearMap);
				}

				//for each energy type
				for (EnergyType et: EnergyType.values()) {

					HashMap<String, Double> zoneMap = yearMap.get(et);
					if (zoneMap == null) {
						zoneMap = new HashMap<String, Double>();
						yearMap.put(et, zoneMap);
					}

					consumption = Double.parseDouble(record.get(et));
					zoneMap.put(zone, consumption);
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

		LOGGER.debug("Zonal car energy consumptions read from file with data points for {} years and {} zones.", map.keySet().size(), zonesNumber);

		return map;
	}
}
