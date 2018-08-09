package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.Trip;

/**
 * Origin-destination matrix created from productions, attractions and observed trip length distribution.
 * @author Milan Lovric
 *
 */
public class EstimatedODMatrix extends RealODMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(EstimatedODMatrix.class);
	
	//public static final int[] BIN_LIMITS_MILES = {0, 1, 2, 5, 10, 25, 50, 100, 150, 250, 350};
	//public static final double[] BIN_LIMITS_KM = {0.0, 0.621371, 1.242742, 3.106855, 6.21371, 15.534275, 31.06855, 62.1371, 93.20565, 155.34275, 217.47985};
	//public static final double[] OTLD = {0.05526, 0.16579, 0.34737, 0.21053, 0.15789, 0.03947, 0.01579, 0.00432, 0.00280, 0.00063, 0.00014};

	private final double[] binLimitsKm;
	private final double[] observedTripLengthDistribution;

	private HashMap<String, Integer> productions;
	private HashMap<String, Integer> attractions;
	private List<String> zones;

	private ODMatrix binIndexMatrix;
	private double[] tripLengthDistribution;
	private RoadNetworkAssignment rna;

	/**
	 * Constructor for estimated OD matrix.
	 * @param productions Productions
	 * @param attractions Attractions
	 * @param distanceSkimMatrix Distance skim matrix
	 * @param binLimitsKm Bin limits in km
	 * @param observedTripLengthDistribution Observed trip length distribution (normalised).
	 */
	public EstimatedODMatrix(HashMap<String, Integer> productions, HashMap<String, Integer>attractions, SkimMatrix distanceSkimMatrix,
			final double[] binLimitsKm, final double[] observedTripLengthDistribution) {

		super();

		this.binLimitsKm = binLimitsKm;
		this.observedTripLengthDistribution = observedTripLengthDistribution;
		this.productions = productions;
		this.attractions = attractions;
		this.zones = new ArrayList<String>();
		this.binIndexMatrix = new ODMatrix();
		this.tripLengthDistribution = new double[observedTripLengthDistribution.length];

		for (String zone: productions.keySet()) zones.add(zone);

		System.out.println("Zones = " + zones);
		System.out.println("Productions = " + productions);
		System.out.println("Attractions = " + attractions);

		//check validity
		int sumProductions = 0, sumAttractions = 0;
		for (String zone: zones) {
			sumProductions += this.productions.get(zone);
			sumAttractions += this.attractions.get(zone);
		}
		if (sumProductions != sumAttractions) System.err.println("The sum of productions does not equal the sum of attractions!");

		//determine binning based on the distanceSkimMatrix (this will not change for a given distance skim matrix).
	//	this.determineBinIndexMatrix(distanceSkimMatrix);
		//this.binIndexMatrix.printMatrixFormatted();

		createUnitMatrix();
	//	iterate();
	}
	
	/**
	 * Constructor for estimated OD matrix that reads productions and attractions from an input csv file.
	 * @param fileName Path to the input file with productions and attractions
	 * @param distanceSkimMatrix Distance skim matrix
	 * @param binLimitsKm Bin limits in km
	 * @param observedTripLengthDistribution Observed trip length distribution (normalised).
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public EstimatedODMatrix(String fileName, SkimMatrix distanceSkimMatrix, final double[] binLimitsKm, final double[] observedTripLengthDistribution) throws FileNotFoundException, IOException {

		super();

		this.binLimitsKm = binLimitsKm;
		this.observedTripLengthDistribution = observedTripLengthDistribution;
		this.productions = new HashMap<String, Integer>();
		this.attractions = new HashMap<String, Integer>();
		this.zones = new ArrayList<String>();
		this.binIndexMatrix = new ODMatrix();
		this.tripLengthDistribution = new double[observedTripLengthDistribution.length];

		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		System.out.println("keySet = " + keySet);
		int production, attraction;
		for (CSVRecord record : parser) { 
			System.out.println(record);
			System.out.println("Zone  = " + record.get(0));
			production = Integer.parseInt(record.get("Production"));
			attraction = Integer.parseInt(record.get("Attraction"));
			productions.put(record.get(0), production);
			attractions.put(record.get(0), attraction);
			zones.add(record.get(0));

		}
		parser.close(); 

		System.out.println("Zones = " + zones);
		System.out.println("Productions = " + productions);
		System.out.println("Attractions = " + attractions);

		//check validity
		int sumProductions = 0, sumAttractions = 0;
		for (String zone: zones) {
			sumProductions += this.productions.get(zone);
			sumAttractions += this.attractions.get(zone);
		}
		if (sumProductions != sumAttractions) System.err.println("The sum of productions does not equal the sum of attractions!");

		//determine binning based on the distanceSkimMatrix (this will not change for a given distance skim matrix).
		this.determineBinIndexMatrix(distanceSkimMatrix);
		//this.binIndexMatrix.printMatrixFormatted();

		createUnitMatrix();
		iterate();
	}
	
	/**
	 * Constructor for estimated OD matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param zones List of zones.
	 * @param rna Road network assignment.
	 */
	public EstimatedODMatrix(List<String> zones, RoadNetworkAssignment rna) {

		super();
		
		this.binLimitsKm = null;
		this.observedTripLengthDistribution = null;
		this.rna = rna;
		this.zones = new ArrayList<String>();
		
		for (String zone: zones) this.zones.add(zone);
		this.createUnitMatrix();
	}

	/**
	 * Iterates scaling to productions, scaling to attractions, rounding and scaling to observed trip length distribution.
	 */
	public void iterate() {

		scaleToProductions();
		scaleToAttractions();
		scaleToObservedTripLenghtDistribution();
		this.roundMatrixValues();
		updateTripLengthDistribution();
	}


	/**
	 * Creates a unit OD matrix (all ones).
	 */
	public void createUnitMatrix() {

		for (String origin: this.zones) 
			for (String destination: this.zones)
				this.setFlow(origin, destination, 1);
	}

	/**
	 * Scales OD matrix to productions.
	 */
	public void scaleToProductions() {

		for (String origin: zones) {
			//int sumFlows = 0;
			double sumFlows = 0.0;
			for (String destination: zones) 
				sumFlows += this.getFlow(origin, destination);
			for (String destination: zones) {
				//int newFlow = (int) Math.round((double) this.getFlow(origin, destination) * this.productions.get(origin) / sumFlows);
				double newFlow = this.getFlow(origin, destination) * this.productions.get(origin) / sumFlows;
				this.setFlow(origin,  destination, newFlow);
			}
		}
	}

	/**
	 * Scales OD matrix to attractions.
	 */
	public void scaleToAttractions() {

		for (String destination: zones) {
			//int sumFlows = 0;
			double sumFlows = 0.0;
			for (String origin: zones) 
				sumFlows += this.getFlow(origin, destination);
			for (String origin: zones) {
				//int newFlow = (int) Math.round((double) this.getFlow(origin, destination) * this.attractions.get(destination) / sumFlows);
				double newFlow = (double) this.getFlow(origin, destination) * this.attractions.get(destination) / sumFlows;
				this.setFlow(origin,  destination, newFlow);
			}
		}
	}

	/**
	 * Scales OD matrix to observed trip length distribution.
	 */
	public void scaleToObservedTripLenghtDistribution() {

		updateTripLengthDistribution(); //this needs to be updated every time the OD matrix changes.

		for (String o: this.zones)
			for (String d: this.zones) {
				//int flow = this.getFlow(o, d);
				double flow = this.getFlow(o, d);
				int binIndex = this.binIndexMatrix.getFlow(o, d) - 1;
				if (binIndex >= 0) {
					//flow = (int) Math.round(flow * OTLD[binIndex] / tripLengthDistribution[binIndex]);
					flow = flow * observedTripLengthDistribution[binIndex] / tripLengthDistribution[binIndex];
					this.setFlow(o, d, flow);
				}
			}
	}

	/**
	 * Scales OD matrix to traffic counts.
	 */
	public void scaleToTrafficCounts() {
			
		this.rna.resetLinkVolumes();
		this.rna.resetTripStorages();
		ODMatrix odm = new ODMatrix(this);
		
		this.rna.assignPassengerFlowsRouting(odm, null);
		double RMSN = this.rna.calculateRMSNforSimulatedVolumes();
		System.out.printf("RMSN before scaling = %.2f%% %n", RMSN);
		
		RealODMatrix sf = this.getScalingFactors();
		sf.printMatrixFormatted("Scaling factors:", 4);
		
		this.scaleMatrixValue(sf);
		this.printMatrixFormatted("OD matrix after scaling:", 2);
	}
	
	/**
	 * Calculates scaling factors for OD pairs.
	 * @return Scaling factors.
	 */
	public RealODMatrix getScalingFactors() {
		
		List<Trip> tripList = this.rna.getTripList();
		
		Map<Integer, Integer> volumes = this.rna.getLinkVolumePerVehicleType().get(VehicleType.CAR);
		Map<Integer, Integer> counts = this.rna.getAADFCarTrafficCounts();
		Map<Integer, Double> linkFactors = new HashMap<Integer, Double>();
		
		for (Integer edgeID: volumes.keySet()) linkFactors.put(edgeID, 1.0 * counts.get(edgeID) / volumes.get(edgeID));
		
		RealODMatrix factors = new RealODMatrix();
		ODMatrix counter = new ODMatrix();
		RealODMatrix scalingFactors = new RealODMatrix();
		
		for (Trip t: tripList) 
			if (t.getVehicle().equals(VehicleType.CAR))	{
			
				String originLAD = t.getOriginLAD(this.rna.getRoadNetwork().getNodeToZone());
				String destinationLAD = t.getDestinationLAD(this.rna.getRoadNetwork().getNodeToZone());
				Route route = t.getRoute();
				int multiplier = t.getMultiplier();
				
				//get current factor and count
				double factor = factors.getFlow(originLAD, destinationLAD); 
				int count = counter.getFlow(originLAD, destinationLAD);
				
				for (int edgeID: route.getEdges().toArray()) {
					factor += linkFactors.get(edgeID) * multiplier;
					count += multiplier;
				}
				
				//update factor sum and count
				factors.setFlow(originLAD, destinationLAD, factor);
				counter.setFlow(originLAD, destinationLAD, count);
			}
		
		//calculate scaling factors by dividing factor sum and counter
		for (Object mk: factors.getKeySet()) {
			String originLAD = (String) ((MultiKey)mk).getKey(0);
			String destinationLAD = (String) ((MultiKey)mk).getKey(1);
	
			double scalingFactor = 1.0 * factors.getFlow(originLAD, destinationLAD) / counter.getFlow(originLAD, destinationLAD);
			scalingFactors.setFlow(originLAD, destinationLAD, scalingFactor);
		}
		
		return scalingFactors;
	}
	
	/**
	 * Determines which OD pairs fall within which bin index (depending on the distance skim matrix).
	 * @param distanceSkimMatrix Distance skim matrix
	 */
	private void determineBinIndexMatrix(SkimMatrix distanceSkimMatrix) {

		for (String o: this.zones)
			for (String d: this.zones) {

				Double distance = distanceSkimMatrix.getCost(o, d);
				if (distance == null) {
					System.err.printf("No distance from %s to %s\n", o, d);
					continue;
				}

				for (int i=1; i<binLimitsKm.length; i++) {
					if (distance < binLimitsKm[i]) {
						binIndexMatrix.setFlow(o, d, i);
						break;
					}
					binIndexMatrix.setFlow(o, d, binLimitsKm.length);
				}
			}
	}
	
	/**
	 * Updates trip length distribution (using the current values of the OD matrix).
	 */
	public void updateTripLengthDistribution() {

		for (int i=0; i<tripLengthDistribution.length; i++) tripLengthDistribution[i] = 0.0;

		for (String o: this.zones)
			for (String d: this.zones) {
				double flow = this.getFlow(o, d);
				int binIndex = binIndexMatrix.getFlow(o, d) - 1;
				if (binIndex >= 0) tripLengthDistribution[binIndex] += flow;
				else System.err.println("Bin index out of bounds.");
			}

		//normalise
		double sum = 0.0;
		for (int i=0; i<tripLengthDistribution.length; i++) sum += tripLengthDistribution[i];
		for (int i=0; i<tripLengthDistribution.length; i++) tripLengthDistribution[i] /= sum;
	}

	/**
	 * Getter method for the bin index matrix.
	 * @return Bin index matrix
	 */
	public ODMatrix getBinIndexMatrix() {

		return this.binIndexMatrix;
	}

	/**
	 * Getter method for the trip length distribution.
	 * @return Trip length distribution
	 */
	public double[] getTripLengthDistribution() {

		return this.tripLengthDistribution;
	}

	/**
	 * Getter method for the observed trip length distribution.
	 * @return Observed trip length distribution
	 */
	public double[] getObservedTripLengthDistribution() {

		return this.observedTripLengthDistribution;
	}

	/**
	 * Getter method for the productions.
	 * @return Productions
	 */
	public HashMap<String, Integer> getProductions() {

		return this.productions;
	}

	/**
	 * Getter method for the attractions.
	 * @return Attractions
	 */
	public HashMap<String, Integer> getAttractions() {

		return this.attractions;
	}

	/**
	 * Prints the matrix as a formatted table.
	 */
	@Override
	public void printMatrixFormatted(int precision) {

		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);

		//formatted print
		System.out.print("origin    "); for (String s: secondKeyList) System.out.printf("%10s",s);	System.out.println("  Product.");
		for (String o: firstKeyList) {
			System.out.printf("%-10s", o);
			for (String s: secondKeyList) System.out.printf("%10." + precision + "f", this.getFlow(o,s));
			System.out.printf("%10d\n", this.productions.get(o));
		}
		System.out.print("Attract.  "); for (String s: secondKeyList) System.out.printf("%10d", this.attractions.get(s));
		System.out.println();
	}
	
	/**
	 * Prints the message and the matrix as a formatted table.
	 */
	@Override
	public void printMatrixFormatted(String message, int precision) {
		
		System.out.println(message);
		this.printMatrixFormatted(precision);
	}
	
	/**
	 * Deletes all inter-zonal flows to/from a particular zone (leaving only intra-zonal flows)
	 * @param zone Zone for which inter-zonal flows need to be deleted from the origin-destination matrix.
	 */
	public void deleteInterzonalFlows(String zone) {
		
		for (String zone2: this.zones) 
			if (!zone2.equals(zone)) {
				this.setFlow(zone, zone2, 0.0);
				this.setFlow(zone2,  zone, 0.0);
			}
	}
}
