package nismod.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.jfree.data.category.DefaultCategoryDataset;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.decision.VehicleElectrification;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.RealODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.optimisation.SPSA3;
import nismod.transport.visualisation.LineVisualiser;

/**
 * NISMOD V2.0.0 Transport Model
 * @author Milan Lovric
 *
 */
public class App {
	
	private final static Logger LOGGER = Logger.getLogger(App.class.getName());

	public static void main( String[] args ) {

		System.out.println( "NISMOD V2.0.0 Transport Model" );

		try {
			Properties props = new Properties();
			//final String propFileName = "./src/main/resources/config.properties";
			//final String propFileName = "./src/test/resources/config.properties";
			final String propFileName = args[0];

			InputStream inputStream = null;
			try {
				inputStream = new FileInputStream(propFileName);
				// load properties file
				props.load(inputStream);
			} catch (IOException ex) {
				ex.printStackTrace();
				System.err.println("Unable to load config.properties. Using default values.");
				setDefaultProperties(props);

			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			final String fromYear = props.getProperty("fromYear");
			final String predictedYear = props.getProperty("predictedYear");

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

			final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
			final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
			final String populationFile = props.getProperty("populationFile");
			final String GVAFile = props.getProperty("GVAFile");
			final String elasticitiesFile = props.getProperty("elasticitiesFile");
			final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

			final String roadExpansionFileName = props.getProperty("roadExpansionFile");
			final String vehicleElectrificationFileName = props.getProperty("vehicleElectrificationFile");

			final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
			final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
			final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");

			final String energyConsumptionsFile = props.getProperty("energyConsumptionsFile"); //output

			final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
			final String freightRoutesFile = props.getProperty("freightRoutesFile");
			final String outputFolder = props.getProperty("outputFolder");
			final String assignmentResultsFile = props.getProperty("assignmentResultsFile");

			//create output directory
			File file = new File(outputFolder);
			if (!file.exists()) {
				if (file.mkdirs()) {
					System.out.println("Output directory is created.");
				} else {
					System.err.println("Failed to create output directory.");
				}
			}

			//create a road network
			RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
			roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
			roadNetwork.makeEdgesAdmissible();

			if (args.length == 1) {//run the demand model

				//load interventions
				List<Intervention> interventions = new ArrayList<Intervention>();
				RoadExpansion re = new RoadExpansion(roadExpansionFileName);
				VehicleElectrification ve = new VehicleElectrification(vehicleElectrificationFileName);
				interventions.add(re);
				interventions.add(ve);

				RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
				rsg.readRoutesBinary(passengerRoutesFile);
				rsg.readRoutesBinary(freightRoutesFile);

				//the main demand model
				DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, props);
				dm.predictHighwayDemand(Integer.parseInt(predictedYear), Integer.parseInt(fromYear));
				dm.saveEnergyConsumptions(Integer.parseInt(predictedYear), energyConsumptionsFile);

			} else {//there are additional parameters

				final String flag = args[1]; //read the flag
				final String sliceIndex;
				final String sliceNumber;
				String topNodes = "";
				RouteSetGenerator routes;
				final String maxNumberOfIterations;

				if (flag.charAt(0) == '-') { 
					switch (flag.charAt(1)) { 
					case 'r':  //generate routes for the passenger demand
						if (args.length < 4) throw new IllegalArgumentException();
						else {
							sliceIndex = args[2];
							sliceNumber = args[3];
						} 

						if (args.length == 5) topNodes = args[4];

						roadNetwork.sortGravityNodes();
						routes = new RouteSetGenerator(roadNetwork, props);
						ODMatrix passengerODM = new ODMatrix(baseYearODMatrixFile);

						if (!topNodes.isEmpty()) {
							//generate only between top nodes
							System.out.printf("Generating routes for slice %s out of %s for %s top nodes \n", sliceIndex, sliceNumber, topNodes);
							routes.generateRouteSetForODMatrix(passengerODM, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber), Integer.parseInt(topNodes));
							routes.saveRoutesBinary("routes" + sliceIndex + "of" + sliceNumber + "top" + topNodes + ".dat", false);
						} else { 
							//generate between all nodes
							System.out.printf("Generating routes for slice %s out of %s \n", sliceIndex, sliceNumber);
							routes.generateRouteSetForODMatrix(passengerODM, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber));
							routes.saveRoutesBinary("routes" + sliceIndex + "of" + sliceNumber + ".dat", false);
						}
						break;
					case 'f': //generate routes for the freight demand
						if (args.length < 4) throw new IllegalArgumentException();
						else {
							sliceIndex = args[2];
							sliceNumber = args[3];
						} 

						if (args.length == 5) topNodes = args[4];

						roadNetwork.sortGravityNodesFreight();
						routes = new RouteSetGenerator(roadNetwork, props);
						FreightMatrix freightMatrix = new FreightMatrix(baseYearFreightMatrixFile);

						if (!topNodes.isEmpty()) {
							//generate only between top nodes
							System.out.printf("Generating routes for slice %s out of %s for %s top nodes \n", sliceIndex, sliceNumber, topNodes);
							routes.generateRouteSetForFreightMatrix(freightMatrix, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber), Integer.parseInt(topNodes));
							routes.saveRoutesBinary("freightRoutes" + sliceIndex + "of" + sliceNumber + "top" + topNodes + ".dat", false);
						} else { 
							//generate between all nodes
							System.out.printf("Generating routes for slice %s out of %s \n", sliceIndex, sliceNumber);
							routes.generateRouteSetForFreightMatrix(freightMatrix, Integer.parseInt(sliceIndex), Integer.parseInt(sliceNumber));
							routes.saveRoutesBinary("freightRoutes" + sliceIndex + "of" + sliceNumber + ".dat", false);
						}
						break;
					case 'e':  //estimate OD matrix using SPSA optimisation
						if (args.length < 3) throw new IllegalArgumentException();
						else {
							maxNumberOfIterations = args[2];
						} 

						roadNetwork.sortGravityNodes();
						//initial OD matrix
						RealODMatrix odmatrix = new RealODMatrix(baseYearODMatrixFile);
						//create a road network assignment
						RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
						//create route set
						RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
						//read only if route choice version should be used
						if (rna.getFlagUseRouteChoiceModel()) {
							rsg.readRoutesBinary(passengerRoutesFile);
							//rsg.readRoutesBinary(freightRoutesFile);
						}

						double a1 = 100000;
						double A1 = 0.0; 
						double c1 = 500;
						double a2 = 0.0001;
						double A2 = 0.0; 
						double c2 = 0.01; 
						double alpha = 0.602;
						double gamma = 0.101;

						SPSA3 optimiser = new SPSA3();
						optimiser.initialise(rna, rsg, props, odmatrix, rna.getStartNodeProbabilities(), rna.getEndNodeProbabilities(), a1, A1, c1, a2, A2, c2, alpha, gamma);
						optimiser.runSPSA(Integer.parseInt(maxNumberOfIterations));

						optimiser.getThetaEstimate().printMatrixFormatted("Final OD matrix:");
						System.out.println("Final start node probabilities: " + optimiser.getThetaEstimateStart());
						System.out.println("Final end node probabilities: " + optimiser.getThetaEstimateEnd());
						optimiser.getThetaEstimate().saveMatrixFormatted(outputFolder + "finalODMatrix.csv");
						optimiser.saveNodeProbabilities(outputFolder + "nodeProbabilities.csv");

						System.out.printf("Final RMSN: %.2f%% %n", optimiser.lossFunction());
						System.out.printf("Final RMSN: %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(1.0));
						System.out.printf("Final MAD: %.2f%% %n", rna.calculateMADforExpandedSimulatedVolumes(1.0));

						DefaultCategoryDataset lineDataset3 = new DefaultCategoryDataset();
						List<Double> lossEvals3 = optimiser.getLossFunctionEvaluations();
						for (int i = 0; i < lossEvals3.size(); i++) lineDataset3.addValue(lossEvals3.get(i), "RMSN", Integer.toString(i));
						LineVisualiser line3 = new LineVisualiser(lineDataset3, "Loss function evaluations");
						line3.setSize(600, 400);
						line3.setVisible(true);
						line3.saveToPNG(outputFolder + "evaluations.png");
						break;
					default:  throw new IllegalArgumentException();
					}
				}	else throw new IllegalArgumentException();

			}
		} catch (IllegalArgumentException e) {
			System.err.println("Correct arguments for route generation should be one of the following:");
			System.err.println("<path>/config.properties -r sliceIndex sliceNumber");
			System.err.println("<path>/config.properties -r sliceIndex sliceNumber topNumbers");
			System.err.println("<path>/config.properties -f sliceIndex sliceNumber");
			System.err.println("<path>/config.properties -f sliceIndex sliceNumber topNumbers");
			System.err.println("<path>/config.properties -e maxNumberOfIterations");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Done.");
		}
	}

	private static void setDefaultProperties(Properties props) {

		props.setProperty("baseYear", "2015");
		props.setProperty("predictedYear", "2016");

		props.setProperty("zonesUrl", "file://src/test/resources/testdata/zones.shp");
		props.setProperty("networkUrl", "file://src/test/resources/testdata/network.shp");
		props.setProperty("networkUrlFixedEdgeIDs", "file://src/test/resources/testdata/testOutputNetwork.shp");
		props.setProperty("nodesUrl", "file://src/test/resources/testdata/nodes.shp");
		props.setProperty("AADFurl", "file://src/test/resources/testdata/AADFdirected.shp");

		props.setProperty("areaCodeFileName", "./src/test/resources/testdata/nomisPopulation.csv");
		props.setProperty("areaCodeNearestNodeFile", "./src/test/resources/testdata/areaCodeToNearestNode.csv");
		props.setProperty("workplaceZoneFileName", "./src/test/resources/testdata/workplacePopulation.csv");
		props.setProperty("workplaceZoneNearestNodeFile", "./src/test/resources/testdata/workplaceZoneToNearestNode.csv");
		props.setProperty("freightZoneToLADfile", "./src/test/resources/testdata/freightZoneToLAD.csv");
		props.setProperty("freightZoneNearestNodeFile", "./src/test/resources/testdata/freightZoneToNearestNode.csv");

		props.setProperty("baseYearODMatrixFile", "./src/test/resources/testdata/passengerODM.csv");
		props.setProperty("baseYearFreightMatrixFile", "./src/test/resources/testdata/freightMatrix.csv");
		props.setProperty("populationFile", "./src/test/resources/testdata/population.csv");
		props.setProperty("GVAFile", "./src/test/resources/testdata/GVA.csv");

		props.setProperty("energyUnitCostsFile", "./src/test/resources/testdata/energyUnitCosts.csv");
		props.setProperty("engineTypeFractionsFile", "./src/test/resources/testdata/engineTypeFractionsFile.csv");
		props.setProperty("energyConsumptionsFile", "./energyConsumptions.csv");

		props.setProperty("roadExpansionFileName", "./src/test/resources/testdata/roadExpansion.properties");
		props.setProperty("vehicleElectrificationFileName", "./src/test/resources/testdata/vehicleEletrification.properties");
	}
}
