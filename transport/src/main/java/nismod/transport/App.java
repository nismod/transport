package nismod.transport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.decision.VehicleElectrification;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RouteSetGenerator;

/**
 * NISMOD V2.0.0 Transport Model
 * @author Milan Lovric
 *
 */
public class App {

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
			
			final String baseYear = props.getProperty("baseYear");
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
	
			final String roadExpansionFileName = props.getProperty("roadExpansionFileName");
			final String vehicleElectrificationFileName = props.getProperty("vehicleElectrificationFileName");

			final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
			final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
			final String energyConsumptionsFile = props.getProperty("energyConsumptionsFile"); //output
			
			//create a road network
			RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
			roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
			roadNetwork.makeEdgesAdmissible();
						
			if (args.length == 1) {//run the demand model

				//load interventions
				List<Intervention> interventions = new ArrayList<Intervention>();
				RoadExpansion re = new RoadExpansion(roadExpansionFileName);
				VehicleElectrification ve = new VehicleElectrification(vehicleElectrificationFileName);
				interventions.add(re);
				interventions.add(ve);

				//the main demand model
				DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, energyUnitCostsFile, engineTypeFractionsFile, interventions, null, null);
				dm.predictHighwayDemand(Integer.parseInt(predictedYear), Integer.parseInt(baseYear));
				dm.saveEnergyConsumptions(Integer.parseInt(predictedYear), energyConsumptionsFile);
				
			} else {//there are additional parameters
				
				final String flag = args[1]; //read the flag
				final String sliceIndex;
				final String sliceNumber;
				String topNodes = "";
				RouteSetGenerator routes;
				
				if (flag.charAt(0) == '-') { 
					switch (flag.charAt(1)) { 
						case 'r': 
								  if (args.length < 4) throw new IllegalArgumentException();
								  else {
									  sliceIndex = args[2];
								  	  sliceNumber = args[3];
									} 
															
								  if (args.length == 5) topNodes = args[4];
								  
								  roadNetwork.sortGravityNodes();
								  routes = new RouteSetGenerator(roadNetwork);
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
						case 'f': 
							  if (args.length < 4) throw new IllegalArgumentException();
							  else {
								  sliceIndex = args[2];
							  	  sliceNumber = args[3];
								} 
							  
							  if (args.length == 5) topNodes = args[4];
							  
							  roadNetwork.sortGravityNodesFreight();
							  routes = new RouteSetGenerator(roadNetwork);
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
								  routes.saveRoutes("freightRoutes" + sliceIndex + "of" + sliceNumber + ".dat", false);
							  }
							  break;
						default:  throw new IllegalArgumentException();
					}
				}	else throw new IllegalArgumentException();
				
			}
		} catch (IllegalArgumentException e) {
			System.err.println("Correct arguments for route generation should be one of the following:");
			System.err.println("<path>/config.properties -r sliceIndex sliceNumber");
			System.err.println("<path>/config.properties -r sliceIndex sliceNumber topNumbers");
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
