package nismod.transport.optimisation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.jfree.data.category.DefaultCategoryDataset;

import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.RealODMatrix;
import nismod.transport.demand.RealODMatrixTempro;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.visualisation.LineVisualiser;
import nismod.transport.visualisation.NetworkVisualiser;
import nismod.transport.zone.Zoning;

/**
 * Tests for the SPSA class
 * @author Milan Lovric
 *
 */
public class SPSATest {

	public static void main( String[] args ) throws IOException	{
			
		//final String configFile = "./src/main/config/config.properties";
		final String configFile = "./src/test/config/testConfig.properties";
		//final String configFile = "./src/test/config/minitestConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
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
		final String freightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
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
		
		//set nodes probability weighting
		props.setProperty("NODES_PROBABILITY_WEIGHTING", Double.toHexString(0.5));
		//set inter-zonal top nodes
		props.setProperty("INTERZONAL_TOP_NODES", "5");
		
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String timeOfDayDistributionFreightFile = props.getProperty("timeOfDayDistributionFreightFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
	
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, 
															InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile).get(BASE_YEAR),
															InputFileReader.readUnitCO2EmissionFile(unitCO2EmissionsFile).get(BASE_YEAR),
															InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile).get(BASE_YEAR),
															InputFileReader.readAVFractionsFile(AVFractionsFile).get(BASE_YEAR),
															InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile),
															InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile),
															InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFreightFile).get(BASE_YEAR),
															null,
															null,
															null,
															null,
															props);
	
	/*	
	
		//initial OD matrix
		RealODMatrix odm = new RealODMatrix(baseYearODMatrixFile);
		//odm.setFlow("E06000045", "E06000045", 70000);
		//odm.scaleMatrixValue(5.0);
		odm.printMatrixFormatted("Initial passenger matrix:");
		
		//double a = 100000;
		double a = 10000000;
		double A = 0.0; 
		double c = 500; 
		double alpha = 0.602;
		double gamma = 0.101;
		
		SPSA optimiser = new SPSA();
		optimiser.initialise(rna, odm, a, A, c, alpha, gamma);
		optimiser.runSPSA(100);
		
		optimiser.getThetaEstimate().printMatrixFormatted("Final OD matrix:");
	
		System.out.printf("Final RMSN: %.2f%% %n", optimiser.lossFunction());
		System.out.printf("Final RMSN: %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(1.0));
		System.out.printf("Final MAD: %.2f%% %n", rna.calculateMADforExpandedSimulatedVolumes(1.0));
		
		System.out.printf("Final RMSN: %.2f%% %n", optimiser.lossFunction());
		
		
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		List<Double> lossEvals = optimiser.getLossFunctionEvaluations();
		for (int i = 0; i < lossEvals.size(); i++) lineDataset.addValue(lossEvals.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line = new LineVisualiser(lineDataset, "Loss function evaluations");
		line.setSize(600, 400);
		line.setVisible(true);
		//line.saveToPNG("LineVisualiserTest.png");
//				
//		rna.updateLinkVolumeInPCU();
//		Map<Integer, Double> dailyVolume = rna.getLinkVolumesInPCU();
//		System.out.println(dailyVolume);
		
		//NetworkVisualiser.visualise(roadNetwork, "Network from shapefiles");
		//NetworkVisualiser.visualise(roadNetwork, "Network with traffic volume", dailyVolume);
		//NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateAbsDifferenceCarCounts());
		
//		
//		//set nodes probability weighting
//		props.setProperty("NODES_PROBABILITY_WEIGHTING", Double.toString(0.5));
//		//set inter-zonal top nodes
//		props.setProperty("INTERZONAL_TOP_NODES", "5");
//		//create a road network assignment
//		rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
		
		
	
		
		//initial OD matrix
		ODMatrix odmat = new ODMatrix(optimiser.getThetaEstimate());
		//ODMatrix odmat = new ODMatrix(baseYearODMatrixFile);
		//odmat.setFlow("E06000045", "E06000045", 70269);
		//odmat.setFlow("E06000045", "E06000045", 70000);
		odmat.printMatrixFormatted("Initial passenger matrix:");
		
		a = 0.0001;
		A = 0.0; 
		c = 0.01; 
		alpha = 0.602;
		gamma = 0.101;
		
		SPSA2 optimiser2 = new SPSA2();
		optimiser2.initialise(rna, odmat, rna.getStartNodeProbabilities(), rna.getEndNodeProbabilities(), a, A, c, alpha, gamma);
		optimiser2.runSPSA(100);

		System.out.printf("Final RMSN: %.2f%% %n", optimiser2.lossFunction());
		
		DefaultCategoryDataset lineDataset2 = new DefaultCategoryDataset();
		List<Double> lossEvals2 = optimiser2.getLossFunctionEvaluations();
		for (int i = 0; i < lossEvals2.size(); i++) lineDataset2.addValue(lossEvals2.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line2 = new LineVisualiser(lineDataset2, "Loss function evaluations");
		line2.setSize(600, 400);
		line2.setVisible(true);
		
		*/
		
		
		/*
	
		//initial OD matrix
		RealODMatrix odmatrix = new RealODMatrix(baseYearODMatrixFile);
		//RealODMatrix odmatrix = optimiser.getThetaEstimate();
		////odmatrix.setFlow("E06000045", "E06000045", 70269);
		////odmatrix.setFlow("E06000045", "E06000045", 70000);
		//odmatrix.setFlow("E06000045", "E06000045", 72930);
		//odmatrix.scaleMatrixValue(10.0);
		odmatrix.printMatrixFormatted("Initial passenger matrix:");
		
		double a1 = 100000;
		double A1 = 0.0; 
		double c1 = 500;
		double a2 = 0.0001;
		double A2 = 0.0; 
		double c2 = 0.01; 
		double alpha = 0.602;
		double gamma = 0.101;
		
		SPSA3 optimiser3 = new SPSA3();

		//rsg.readRoutes("./src/test/resources/testdata/testRoutes.txt");
		//rsg.readRoutes("./src/test/resources/testdata/allRoutes.txt");
		Properties rsgparams = new Properties();
		rsgparams.setProperty("ROUTE_LIMIT", "5");
		rsgparams.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, rsgparams);
		rsg.generateRouteSetForODMatrix(new ODMatrix(odmatrix));
		
		rsg.printStatistics();
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
	
		optimiser3.initialise(rna, rsg, params, odmatrix, rna.getStartNodeProbabilities(), rna.getEndNodeProbabilities(), a1, A1, c1, a2, A2, c2, alpha, gamma);
		optimiser3.runSPSA(100);

		optimiser3.getThetaEstimate().printMatrixFormatted("Final OD matrix:");
		System.out.println("Final start node probabilities: " + optimiser3.getThetaEstimateStart());
		System.out.println("Final end node probabilities: " + optimiser3.getThetaEstimateEnd());
		
		System.out.printf("Final RMSN: %.2f%% %n", optimiser3.lossFunction());
		System.out.printf("Final RMSN: %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(1.0));
		System.out.printf("Final MAD: %.2f%% %n", rna.calculateMADforExpandedSimulatedVolumes(1.0));
				
		DefaultCategoryDataset lineDataset3 = new DefaultCategoryDataset();
		List<Double> lossEvals3 = optimiser3.getLossFunctionEvaluations();
		for (int i = 0; i < lossEvals3.size(); i++) lineDataset3.addValue(lossEvals3.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line3 = new LineVisualiser(lineDataset3, "Loss function evaluations");
		line3.setSize(600, 400);
		line3.setVisible(true);
		
		
		*/
		
	
		//Assign tempro matrix
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		
		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		
		//initial OD matrix
		RealODMatrixTempro temproODMatrix = new RealODMatrixTempro(temproODMatrixFile, zoning);
		//temproODMatrix.printMatrixFormatted("Initial passenger matrix:", 2);
		
		//double a = 10000;
		double a = 1000;
		double A = 0.0; 
		double c = 50;
		double alpha = 0.602;
		double gamma = 0.101;
		
		SPSA4 optimiser = new SPSA4(props);

		//rsg.readRoutes("./src/test/resources/testdata/testRoutes.txt");
		//rsg.readRoutes("./src/test/resources/testdata/allRoutes.txt");
		Properties rsgparams = new Properties();
		rsgparams.setProperty("ROUTE_LIMIT", "5");
		rsgparams.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, rsgparams);
		//rsg.generateRouteSetForODMatrix(new ODMatrix(temproODMatrix));
		//generate single node routes
		rsg.generateSingleNodeRoutes();
		rsg.calculateAllPathsizes();
		//read tempro routes
		final String temproRoutesFile = props.getProperty("temproRoutesFile");
		rsg.readRoutesBinaryWithoutValidityCheck(temproRoutesFile);
		rsg.printStatistics();
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
	
		optimiser.initialise(rna, zoning, rsg, temproODMatrix, a, A, c, alpha, gamma);
		optimiser.runSPSA(10);

		optimiser.getThetaEstimate().printMatrixFormatted("Final OD matrix:", 2);
		
		System.out.printf("Final RMSN: %.2f%% %n", optimiser.lossFunction());
		System.out.printf("Final RMSN: %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(1.0));
		System.out.printf("Final MAD: %.2f%% %n", rna.calculateMADforExpandedSimulatedVolumes(1.0));
				
		//int sumVolumes = rna.getLinkVolumePerVehicleType().get(VehicleType.CAR).values().stream().mapToInt(Number::intValue).sum();
		//int sumCounts = roadNetwork.getAADFCarTrafficCounts().values().stream().mapToInt(Number::intValue).sum();;
		int sumVolumes = Arrays.stream(rna.getLinkVolumePerVehicleType().get(VehicleType.CAR)).sum();
		int sumCounts = Arrays.stream(roadNetwork.getAADFCarTrafficCounts()).filter(Objects::nonNull).mapToInt(Number::intValue).sum();
		System.out.println("Sum of volumes : " + sumVolumes);
		System.out.println("Sum of counts : " + sumCounts);
				
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		List<Double> lossEvals = optimiser.getLossFunctionEvaluations();
		for (int i = 0; i < lossEvals.size(); i++) lineDataset.addValue(lossEvals.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line = new LineVisualiser(lineDataset, "Loss function evaluations");
		line.setSize(600, 400);
		line.setVisible(true);
	}
}
