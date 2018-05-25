package nismod.transport;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadDevelopment;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.showcase.LandingGUI;
import nismod.transport.utility.ConfigReader;

/**
 * NISMOD V2.0.0 Transport Model.
 * @author Milan Lovric
 *
 */
public class App {

	private final static Logger LOGGER = LogManager.getLogger(App.class);

	public static void main( String[] args ) {

		LOGGER.info("NISMOD V2.0.0 Transport Model");

		Options options = new Options();
		options.addOption("h", "help", false, "Show help.")
		.addOption("d", "demo", false, "Run the Showcase Demo");

		Option configFile = Option.builder("c")
				.longOpt("configFile")
				.argName("file" )
				.hasArg()
				.desc("Use given file as the config file.")
				.required()
				.build();
		options.addOption(configFile);

		Option passengerRoutes = Option.builder("p")
				.longOpt("passengerRoutes")
				.argName("SLICE_INDEX> <SLICE_NUMBER> <TOP_NODES")
				.hasArg()
				.numberOfArgs(3)
				.desc("Generate routes for passenger demand.")
				.valueSeparator(' ')
				.build();
		options.addOption(passengerRoutes);

		Option freightRoutes = Option.builder("f")
				.longOpt("freightRoutes")
				.argName("SLICE_INDEX> <SLICE_NUMBER> <TOP_NODES")
				.hasArg()
				.numberOfArgs(3)
				.desc("Generate routes for freight demand.")
				.valueSeparator(' ')
				.build();
		options.addOption(freightRoutes);

		// create the parser
		CommandLineParser parser = new DefaultParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			//interrogate options
			if (line.hasOption("h")) {
				printHelp(options);
				System.exit(0);
			}

			RoadNetwork roadNetwork = null;
			Properties props = null;
			File file = null;
			if (line.hasOption("c")) {
				String path = ((String)line.getParsedOptionValue("configFile"));
				LOGGER.info("Path to the config file: {}", path);

				props = ConfigReader.getProperties(path);

				final String areaCodeFileName = props.getProperty("areaCodeFileName");
				final String areaCodeNearestNodeFile = props.getProperty("areaCodeNearestNodeFile");
				final String workplaceZoneFileName = props.getProperty("workplaceZoneFileName");
				final String workplaceZoneNearestNodeFile = props.getProperty("workplaceZoneNearestNodeFile");
				final String freightZoneToLADfile = props.getProperty("freightZoneToLADfile");
				final String freightZoneNearestNodeFile = props.getProperty("freightZoneNearestNodeFile");

				final URL zonesUrl = new URL(props.getProperty("zonesUrl"));
				final URL networkUrl = new URL(props.getProperty("networkUrl"));
				final URL networkUrlFixedEdgeIDs = new URL(props.getProperty("networkUrlFixedEdgeIDs"));
				final URL nodesUrl = new URL(props.getProperty("nodesUrl"));
				final URL AADFurl = new URL(props.getProperty("AADFurl"));

				final String outputFolder = props.getProperty("outputFolder");

				//create output directory
				file = new File(outputFolder);
				if (!file.exists()) {
					if (file.mkdirs()) {
						LOGGER.info("Output directory is created.");
					} else {
						LOGGER.error("Failed to create output directory.");
					}
				}

				//create a road network
				roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
				roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
				roadNetwork.makeEdgesAdmissible();
			}

			if (line.hasOption("p")) {

				String[] values = line.getOptionValues("passengerRoutes");

				final String sliceIndex = values[0];
				final String sliceNumber = values[1];
				final String topNodes = values[2];
				RouteSetGenerator routes;

				roadNetwork.sortGravityNodes();
				routes = new RouteSetGenerator(roadNetwork, props);
				final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
				ODMatrix passengerODM = new ODMatrix(baseYearODMatrixFile);

				if (Integer.parseInt(topNodes) > 0) {
					//generate only between top nodes
					LOGGER.info("Generating routes for slice {} out of {} for {} top nodes.", sliceIndex, sliceNumber, topNodes);
					routes.generateRouteSetForODMatrix(passengerODM, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber), Integer.parseInt(topNodes));
					routes.saveRoutesBinary(file.getPath() + "routes" + sliceIndex + "of" + sliceNumber + "top" + topNodes + ".dat", false);
				} else { 
					//generate between all nodes
					LOGGER.info("Generating routes for slice {} out of {}.", sliceIndex, sliceNumber);
					routes.generateRouteSetForODMatrix(passengerODM, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber));
					routes.saveRoutesBinary(file.getPath() + "routes" + sliceIndex + "of" + sliceNumber + ".dat", false);
				}
				System.exit(0);
			}

			if (line.hasOption("f")) {

				String[] values = line.getOptionValues("freightRoutes");

				final String sliceIndex = values[0];
				final String sliceNumber = values[1];
				final String topNodes = values[2];

				roadNetwork.sortGravityNodesFreight();
				RouteSetGenerator routes = new RouteSetGenerator(roadNetwork, props);
				final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
				FreightMatrix freightMatrix = new FreightMatrix(baseYearFreightMatrixFile);

				if (Integer.parseInt(topNodes) > 0) {
					//generate only between top nodes
					LOGGER.info("Generating routes for slice {} out of {} for {} top nodes.", sliceIndex, sliceNumber, topNodes);
					routes.generateRouteSetForFreightMatrix(freightMatrix, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber), Integer.parseInt(topNodes));
					routes.saveRoutesBinary(file.getPath() + "freightRoutes" + sliceIndex + "of" + sliceNumber + "top" + topNodes + ".dat", false);
				} else { 
					//generate between all nodes
					LOGGER.info("Generating routes for slice {} out of {}.", sliceIndex, sliceNumber);
					routes.generateRouteSetForFreightMatrix(freightMatrix, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber));
					routes.saveRoutesBinary(file.getPath() + "freightRoutes" + sliceIndex + "of" + sliceNumber + ".dat", false);
				}
				System.exit(0);
			}

			if (line.hasOption("d")) LandingGUI.main(null);

			else { //run the main demand prediction model

				final String fromYear = props.getProperty("fromYear");
				final String predictedYear = props.getProperty("predictedYear");

				final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
				final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
				final String populationFile = props.getProperty("populationFile");
				final String GVAFile = props.getProperty("GVAFile");
				final String elasticitiesFile = props.getProperty("elasticitiesFile");
				final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

				final String roadExpansionFile = props.getProperty("roadExpansionFile");
				final String roadDevelopmentFile = props.getProperty("roadDevelopmentFile");
				final String congestionChargingFile = props.getProperty("congestionChargingFile");

				final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
				final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
				final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
				final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");

				final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
				final String freightRoutesFile = props.getProperty("freightRoutesFile");
				
				//load interventions
				List<Intervention> interventions = new ArrayList<Intervention>();
				RoadExpansion re = new RoadExpansion(roadExpansionFile);
				RoadDevelopment rd = new RoadDevelopment(roadDevelopmentFile);
				CongestionCharging cc = new CongestionCharging(congestionChargingFile);
				
				interventions.add(re);
				interventions.add(rd);
				interventions.add(cc);
				
				RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
				
				final Boolean flagUseRouteChoiceModel = Boolean.parseBoolean(props.getProperty("USE_ROUTE_CHOICE_MODEL"));
				if (flagUseRouteChoiceModel) { //if route choice version used, load pre-generated routes
					rsg.readRoutesBinary(passengerRoutesFile);
					rsg.readRoutesBinary(freightRoutesFile);
				}

				//the main demand model
				DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, props);
				dm.predictHighwayDemands(Integer.parseInt(predictedYear), Integer.parseInt(fromYear));
				dm.saveAllResults(Integer.parseInt(predictedYear), Integer.parseInt(fromYear));
			}

		} catch (ParseException e) {
			LOGGER.error(e);
			printHelp(options); 
		} catch (MalformedURLException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
		} finally {
			LOGGER.info("Done.");
		}
	}

	private static void printHelp(Options options) { 

		HelpFormatter formater = new HelpFormatter(); 
		formater.printHelp("App", options); 

	} 
}