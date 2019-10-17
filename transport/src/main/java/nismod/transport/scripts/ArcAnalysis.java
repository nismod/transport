package nismod.transport.scripts;

import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.imaging.ImageWriteException;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.VerticalAlignment;
import org.jfree.data.category.DefaultCategoryDataset;

import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.SkimMatrixMultiKey;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.rail.RailStationDemand;
import nismod.transport.showcase.LandingGUI;
import nismod.transport.utility.InputFileReader;
import nismod.transport.visualisation.BarVisualiser;

/**
 * This scripts parses the outputs for the CaMkOx case study (adjust paths as needed).
 * @author Milan Lovric
 */
public class ArcAnalysis {

	public static final Font TABLE_FONT = new Font("Lato", Font.BOLD, 12);

	final static List<String> arcZones = Arrays.asList("E07000008", "E07000177", "E07000099", "E06000055", "E06000056", 
			"E07000012", "E06000032", "E07000179", "E07000004", "E07000180",
			"E07000181", "E07000155", "E06000042", "E07000178", "E07000151", 
			"E07000154", "E07000156", "E07000009", "E07000242", "E07000011", "E07000243");

	public static void main(String[] args) throws ImageWriteException, IOException {
		// TODO Auto-generated method stub

		System.out.println("Arc zones: " + arcZones);

		//CO2 emissions
		String baseline_2015 = "./output/main/baseline/2015/totalCO2EmissionZonalPerVehicleType.csv";
		String baseline_2030 = "./output/main/baseline/2030/totalCO2EmissionZonalPerVehicleType.csv";
		String baseline_2050 = "./output/main/baseline/2050/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario0_2030 = "./output/main/scenario0/B1/2030/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario0_2050 = "./output/main/scenario0/B1/2050/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario1_2030 = "./output/main/scenario1/B1/2030/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario1_2050 = "./output/main/scenario1/B1/2050/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario2_2030 = "./output/main/scenario2/B1/2030/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario2_2050 = "./output/main/scenario2/B1/2050/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario3_2030 = "./output/main/scenario3/B1/2030/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario3_2050 = "./output/main/scenario3/B1/2050/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario4_2030 = "./output/main/scenario4/B1/2030/totalCO2EmissionZonalPerVehicleType.csv";
		String scenario4_2050 = "./output/main/scenario4/B1/2050/totalCO2EmissionZonalPerVehicleType.csv";


		visualiseCO2Emissions(baseline_2015, baseline_2030, scenario0_2030, scenario1_2030, scenario2_2030, scenario3_2030, 
				scenario4_2030, baseline_2050, scenario0_2050, scenario1_2050, scenario2_2050, scenario3_2050, scenario4_2050);
		
		
		//energy consumptions
		baseline_2015 = "./output/main/baseline/2015/energyConsumptionZonalCar.csv";
		baseline_2030 = "./output/main/baseline/2030/energyConsumptionZonalCar.csv";
		baseline_2050 = "./output/main/baseline/2050/energyConsumptionZonalCar.csv";
		scenario0_2030 = "./output/main/scenario0/B1/2030/energyConsumptionZonalCar.csv";
		scenario0_2050 = "./output/main/scenario0/B1/2050/energyConsumptionZonalCar.csv";
		scenario1_2030 = "./output/main/scenario1/B1/2030/energyConsumptionZonalCar.csv";
		scenario1_2050 = "./output/main/scenario1/B1/2050/energyConsumptionZonalCar.csv";
		scenario2_2030 = "./output/main/scenario2/B1/2030/energyConsumptionZonalCar.csv";
		scenario2_2050 = "./output/main/scenario2/B1/2050/energyConsumptionZonalCar.csv";
		scenario3_2030 = "./output/main/scenario3/B1/2030/energyConsumptionZonalCar.csv";
		scenario3_2050 = "./output/main/scenario3/B1/2050/energyConsumptionZonalCar.csv";
		scenario4_2030 = "./output/main/scenario4/B1/2030/energyConsumptionZonalCar.csv";
		scenario4_2050 = "./output/main/scenario4/B1/2050/energyConsumptionZonalCar.csv";

		visualiseCarEnergyConsumptions(baseline_2015, baseline_2030, scenario0_2030, scenario1_2030, scenario2_2030, scenario3_2030, 
				scenario4_2030, baseline_2050, scenario0_2050, scenario1_2050, scenario2_2050, scenario3_2050, scenario4_2050);

		//time skim matrices
		baseline_2015 = "./output/main/baseline/2015/timeSkimMatrix.csv";
		
		baseline_2030 = "./output/main/baseline/2030/timeSkimMatrix.csv";
		baseline_2050 = "./output/main/baseline/2050/timeSkimMatrix.csv";
		scenario0_2030 = "./output/main/scenario0/B1/2030/timeSkimMatrix.csv";
		scenario0_2050 = "./output/main/scenario0/B1/2050/timeSkimMatrix.csv";
		scenario1_2030 = "./output/main/scenario1/B1/2030/timeSkimMatrix.csv";
		scenario1_2050 = "./output/main/scenario1/B1/2050/timeSkimMatrix.csv";
		
		scenario2_2030 = "./output/main/scenario2/B1/2030/timeSkimMatrix.csv";
		scenario2_2050 = "./output/main/scenario2/B1/2050/timeSkimMatrix.csv";
		scenario3_2030 = "./output/main/scenario3/B1/2030/timeSkimMatrix.csv";
		scenario3_2050 = "./output/main/scenario3/B1/2050/timeSkimMatrix.csv";
		scenario4_2030 = "./output/main/scenario4/B1/2030/timeSkimMatrix.csv";
		scenario4_2050 = "./output/main/scenario4/B1/2050/timeSkimMatrix.csv";

		visualiseTravelTimes(baseline_2030, scenario0_2030, scenario1_2030, scenario2_2030, scenario3_2030, scenario4_2030, 2030);

		visualiseTravelTimes(baseline_2050, scenario0_2050, scenario1_2050, scenario2_2050, scenario3_2050, scenario4_2050, 2050);


		//------------------------------------------------------------------------------------------------
		//RAIL ANALYSIS

		String railDemand2015 = "./output/main/baseline/B1/2015/baseYearRailDemand.csv";
		String railDemand2030 = "./output/main/baseline/B1/2030/predictedRailDemand.csv";
		String railDemand2050 = "./output/main/baseline/B1/2050/predictedRailDemand.csv";

		visualiseRailDemands(railDemand2015, railDemand2030, railDemand2050, "Baseline");

		railDemand2015 = "./output/main/scenario1/B1/2015/baseYearRailDemand.csv";
		railDemand2030 = "./output/main/scenario1/B1/2030/predictedRailDemand.csv";
		railDemand2050 = "./output/main/scenario1/B1/2050/predictedRailDemand.csv";

		visualiseRailDemands(railDemand2015, railDemand2030, railDemand2050, "Scenario 1");

		railDemand2015 = "./output/main/scenario2/B1/2015/baseYearRailDemand.csv";
		railDemand2030 = "./output/main/scenario2/B1/2030/predictedRailDemand.csv";
		railDemand2050 = "./output/main/scenario2/B1/2050/predictedRailDemand.csv";

		visualiseRailDemands(railDemand2015, railDemand2030, railDemand2050, "Scenario 2");

	}

	private static void visualiseRailDemands(String railDemand2015, String railDemand2030, String railDemand2050, String scenario) throws FileNotFoundException, IOException {


		RailStationDemand year2015 = new RailStationDemand(railDemand2015);
		RailStationDemand year2030 = new RailStationDemand(railDemand2030);
		RailStationDemand year2050 = new RailStationDemand(railDemand2050);


		DefaultCategoryDataset barDataset4 = new DefaultCategoryDataset();

		barDataset4.addValue(year2015.getRailDemandMap().get(3115).getDayUsage(), "2015", "OX");
		barDataset4.addValue(year2030.getRailDemandMap().get(3115).getDayUsage(), "2030", "OX");
		barDataset4.addValue(year2050.getRailDemandMap().get(3115).getDayUsage(), "2050", "OX");

		barDataset4.addValue(year2015.getRailDemandMap().get(1378).getDayUsage(), "2015", "MK");
		barDataset4.addValue(year2030.getRailDemandMap().get(1378).getDayUsage(), "2030", "MK");
		barDataset4.addValue(year2050.getRailDemandMap().get(1378).getDayUsage(), "2050", "MK");

		barDataset4.addValue(year2015.getRailDemandMap().get(7022).getDayUsage(), "2015", "CA");
		barDataset4.addValue(year2030.getRailDemandMap().get(7022).getDayUsage(), "2030", "CA");
		barDataset4.addValue(year2050.getRailDemandMap().get(7022).getDayUsage(), "2050", "CA");

		if (year2030.getRailDemandMap().get(500000) != null) {
			barDataset4.addValue(year2030.getRailDemandMap().get(500000).getDayUsage(), "2030", "Winslow");
			barDataset4.addValue(year2050.getRailDemandMap().get(500000).getDayUsage(), "2050", "Winslow");
		}

		//YlOrRd, PRGn, PuOr, RdGy, Spectral, Grays, PuBuGn, RdPu, BuPu, YlOrBr, Greens, BuGn, Accents, GnBu, PuRd, Purples, RdYlGn, Paired, Blues, RdBu, Oranges, RdYlBu, PuBu, OrRd, Set3, Set2, Set1, Reds, PiYG, Dark2, YlGn, BrBG, YlGnBu, Pastel2, Pastel1
		BarVisualiser bar4 = new BarVisualiser(barDataset4, scenario, "Blues", false);
		bar4.setSize(600, 400);
		bar4.setVisible(true);

		ChartPanel cp = (ChartPanel) bar4.getContentPane().getComponent(0);
		JFreeChart chart = cp.getChart();
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);

	}

	private static void visualiseTravelTimes(String timeSkimMatrixFileNO, String timeSkimMatrixFileB1, String timeSkimMatrixFileB3, String scenario, int year) throws FileNotFoundException, IOException {


		SkimMatrix tsmNO = new SkimMatrixMultiKey(timeSkimMatrixFileNO, null);
		SkimMatrix tsmB1 = new SkimMatrixMultiKey(timeSkimMatrixFileB1, null);
		SkimMatrix tsmB3 = new SkimMatrixMultiKey(timeSkimMatrixFileB3, null);

		DefaultCategoryDataset barDataset1 = new DefaultCategoryDataset();

		//OX<->MK (NO)
		double ox2mk = tsmNO.getCost("E07000178", "E06000042");
		double mk2ox = tsmNO.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (NO): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (NO): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (NO): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "NO", "OX ↔ MK");

		//OX<->MK (B1)
		ox2mk = tsmB1.getCost("E07000178", "E06000042");
		mk2ox = tsmB1.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "B1", "OX ↔ MK");

		//OX<->MK (B3)
		ox2mk = tsmB3.getCost("E07000178", "E06000042");
		mk2ox = tsmB3.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B3): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B3): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B3): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "B3", "OX ↔ MK");

		//------------------------------------------------------------------------------------------------

		//MK<->CAM (NO)
		double mk2cam = tsmNO.getCost("E06000042", "E07000008");
		double cam2mk = tsmNO.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (NO): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (NO): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (NO): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "NO", "MK ↔ CAM");

		//MK<->CAM (B1)
		mk2cam = tsmB1.getCost("E06000042", "E07000008");
		cam2mk = tsmB1.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "B1", "MK ↔ CAM");

		//MK<->CAM (B3)
		mk2cam = tsmB3.getCost("E06000042", "E07000008");
		cam2mk = tsmB3.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B3): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B3): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B3): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "B3", "MK ↔ CAM");

		//------------------------------------------------------------------------------------------------

		//OX<->CAM (NO)
		double ox2cam = tsmNO.getCost("E07000178", "E07000008");
		double cam2ox = tsmNO.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (NO): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (NO): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (NO): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "NO", "OX ↔ CAM");

		//OX<->CAM (B1)
		ox2cam = tsmB1.getCost("E07000178", "E07000008");
		cam2ox = tsmB1.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "B1", "OX ↔ CAM");

		//OX<->CAM (B3)
		ox2cam = tsmB3.getCost("E07000178", "E07000008");
		cam2ox = tsmB3.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B3): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B3): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B3): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "B3", "OX ↔ CAM");	

		String title = scenario + " (" + year + ")";
		BarVisualiser bar1 = new BarVisualiser(barDataset1, title, "Set2", true);
		bar1.setSize(600, 400);
		bar1.setVisible(true);
		//line.saveToPNG("BarVisualiserTest2.png");

		ChartPanel cp = (ChartPanel) bar1.getContentPane().getComponent(0);
		JFreeChart chart = cp.getChart();
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);

		Font font3 = new Font("Calibri", Font.PLAIN, 40); 
		chart.getCategoryPlot().getDomainAxis().setLabelFont(font3);
		chart.getCategoryPlot().getRangeAxis().setLabelFont(font3);
	}


	private static void visualiseGeneralisedCost(String timeSkimMatrixFileNO, String timeSkimMatrixFileB1, String timeSkimMatrixFileB3,	String costSkimMatrixFileNO, String costSkimMatrixFileB1, String costSkimMatrixFileB3, String scenario, int year, double vot) throws FileNotFoundException, IOException {

		SkimMatrix tsmNO = new SkimMatrixMultiKey(timeSkimMatrixFileNO, null);
		SkimMatrix tsmB1 = new SkimMatrixMultiKey(timeSkimMatrixFileB1, null);
		SkimMatrix tsmB3 = new SkimMatrixMultiKey(timeSkimMatrixFileB3, null);
		SkimMatrix csmNO = new SkimMatrixMultiKey(costSkimMatrixFileNO, null);
		SkimMatrix csmB1 = new SkimMatrixMultiKey(costSkimMatrixFileB1, null);
		SkimMatrix csmB3 = new SkimMatrixMultiKey(costSkimMatrixFileB3, null);

		DefaultCategoryDataset barDataset1 = new DefaultCategoryDataset();

		//OX<->MK (NO)
		double ox2mk = tsmNO.getCost("E07000178", "E06000042");
		double mk2ox = tsmNO.getCost("E06000042", "E07000178");
		double time = (ox2mk + mk2ox) / 2;
		ox2mk = csmNO.getCost("E07000178", "E06000042");
		mk2ox = csmNO.getCost("E06000042", "E07000178");
		double cost = (ox2mk + mk2ox) / 2;
		double gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "NO", "OX ↔ MK");

		//OX<->MK (B1)
		ox2mk = tsmB1.getCost("E07000178", "E06000042");
		mk2ox = tsmB1.getCost("E06000042", "E07000178");
		time = (ox2mk + mk2ox) / 2;
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		ox2mk = csmB1.getCost("E07000178", "E06000042");
		mk2ox = csmB1.getCost("E06000042", "E07000178");
		cost = (ox2mk + mk2ox) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "B1", "OX ↔ MK");

		//OX<->MK (B3)
		ox2mk = tsmB3.getCost("E07000178", "E06000042");
		mk2ox = tsmB3.getCost("E06000042", "E07000178");
		time = (ox2mk + mk2ox) / 2;
		System.out.printf("Oxford to Milton Keynes (B3): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B3): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B3): %.2f %n", (ox2mk + mk2ox) / 2);
		ox2mk = csmB3.getCost("E07000178", "E06000042");
		mk2ox = csmB3.getCost("E06000042", "E07000178");
		cost = (ox2mk + mk2ox) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "B3", "OX ↔ MK");

		//------------------------------------------------------------------------------------------------

		//MK<->CAM (NO)
		double mk2cam = tsmNO.getCost("E06000042", "E07000008");
		double cam2mk = tsmNO.getCost("E07000008", "E06000042");
		time = (mk2cam + cam2mk) / 2;
		System.out.printf("Milton Keynes to Cambridge (NO): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (NO): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (NO): %.2f %n", (mk2cam + cam2mk) / 2);
		mk2cam = csmNO.getCost("E06000042", "E07000008");
		cam2mk = csmNO.getCost("E07000008", "E06000042");
		cost = (mk2cam + cam2mk) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "NO", "MK ↔ CAM");

		//MK<->CAM (B1)
		mk2cam = tsmB1.getCost("E06000042", "E07000008");
		cam2mk = tsmB1.getCost("E07000008", "E06000042");
		time = (mk2cam + cam2mk) / 2;
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		mk2cam = csmB1.getCost("E06000042", "E07000008");
		cam2mk = csmB1.getCost("E07000008", "E06000042");
		cost = (mk2cam + cam2mk) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "B1", "MK ↔ CAM");

		//MK<->CAM (B3)
		mk2cam = tsmB3.getCost("E06000042", "E07000008");
		cam2mk = tsmB3.getCost("E07000008", "E06000042");
		time = (mk2cam + cam2mk) / 2;
		System.out.printf("Milton Keynes to Cambridge (B3): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B3): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B3): %.2f %n", (mk2cam + cam2mk) / 2);
		mk2cam = csmB3.getCost("E06000042", "E07000008");
		cam2mk = csmB3.getCost("E07000008", "E06000042");
		cost = (mk2cam + cam2mk) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "B3", "MK ↔ CAM");

		//------------------------------------------------------------------------------------------------

		//OX<->CAM (NO)
		double ox2cam = tsmNO.getCost("E07000178", "E07000008");
		double cam2ox = tsmNO.getCost("E07000008", "E07000178");
		time = (ox2cam + cam2ox) / 2;
		System.out.printf("Oxford to Cambridge (NO): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (NO): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (NO): %.2f %n", (ox2cam + cam2ox) / 2);
		ox2cam = csmNO.getCost("E07000178", "E07000008");
		cam2ox = csmNO.getCost("E07000008", "E07000178");
		cost = (ox2cam + cam2ox) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "NO", "OX ↔ CAM");

		//OX<->CAM (B1)
		ox2cam = tsmB1.getCost("E07000178", "E07000008");
		cam2ox = tsmB1.getCost("E07000008", "E07000178");
		time = (ox2cam + cam2ox) / 2;
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		ox2cam = csmB1.getCost("E07000178", "E07000008");
		cam2ox = csmB1.getCost("E07000008", "E07000178");
		cost = (ox2cam + cam2ox) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "B1", "OX ↔ CAM");

		//OX<->CAM (B3)
		ox2cam = tsmB3.getCost("E07000178", "E07000008");
		cam2ox = tsmB3.getCost("E07000008", "E07000178");
		time = (ox2cam + cam2ox) / 2;
		System.out.printf("Oxford to Cambridge (B3): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B3): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B3): %.2f %n", (ox2cam + cam2ox) / 2);
		ox2cam = csmB3.getCost("E07000178", "E07000008");
		cam2ox = csmB3.getCost("E07000008", "E07000178");
		cost = (ox2cam + cam2ox) / 2;
		gen = time * vot / 60 + cost;
		barDataset1.addValue(gen, "B3", "OX ↔ CAM");	

		String title = scenario + " (" + year + ")";
		BarVisualiser bar1 = new BarVisualiser(barDataset1, title, "Set2", true);
		bar1.setSize(600, 400);
		bar1.setVisible(true);
		//line.saveToPNG("BarVisualiserTest2.png");

		ChartPanel cp = (ChartPanel) bar1.getContentPane().getComponent(0);
		JFreeChart chart = cp.getChart();
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);

		Font font3 = new Font("Calibri", Font.PLAIN, 40); 
		chart.getCategoryPlot().getDomainAxis().setLabelFont(font3);
		chart.getCategoryPlot().getRangeAxis().setLabelFont(font3);

	}

	private static void visualiseCO2Emissions (String baseline_2015, String baseline_2030, String scenario0_2030, String scenario1_2030,
			String scenario2_2030, String scenario3_2030, String scenario4_2030, String baseline_2050,
			String scenario0_2050, String scenario1_2050, String scenario2_2050, String scenario3_2050, String scenario4_2050) throws FileNotFoundException, IOException {

		DefaultCategoryDataset barDataset4 = new DefaultCategoryDataset();

		HashMap<Integer, HashMap<VehicleType, HashMap<String, Double>>> map = InputFileReader.readZonalVehicleCO2EmissionsFile(baseline_2015);
		HashMap<VehicleType, HashMap<String, Double>> vehicleMap = map.get(2015);
		double sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		double emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);

		barDataset4.addValue(emission, "Baseline", "2015");
		//barDataset4.addValue(emission, "scenario0", "2015");
		//barDataset4.addValue(emission, "scenario1", "2015");
		//barDataset4.addValue(emission, "scenario2", "2015");
		//barDataset4.addValue(emission, "scenario3", "2015");
		//barDataset4.addValue(emission, "scenario4", "2015");


		map = InputFileReader.readZonalVehicleCO2EmissionsFile(baseline_2030);
		vehicleMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Baseline", "2030");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario0_2030);
		vehicleMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Unplanned", "2030");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario1_2030);
		vehicleMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "New Settlements 30k", "2030");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario2_2030);
		vehicleMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Expansion 30k", "2030");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario3_2030);
		vehicleMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "New Settlements 23k", "2030");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario4_2030);
		vehicleMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Expansion 23k", "2030");


		map = InputFileReader.readZonalVehicleCO2EmissionsFile(baseline_2050);
		vehicleMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Baseline", "2050");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario0_2050);
		vehicleMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Unplanned", "2050");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario1_2050);
		vehicleMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "New Settlements 30k", "2050");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario2_2050);
		vehicleMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Expansion 30k", "2050");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario3_2050);
		vehicleMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "New Settlements 23k", "2050");

		map = InputFileReader.readZonalVehicleCO2EmissionsFile(scenario4_2050);
		vehicleMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += vehicleMap.get(VehicleType.CAR).get(zone);
		emission = sum * 365 / 1000000;
		System.out.printf("Arc CO2 emisions = %.2f kt", emission);
		barDataset4.addValue(emission, "Expansion 23k", "2050");



		//YlOrRd, PRGn, PuOr, RdGy, Spectral, Grays, PuBuGn, RdPu, BuPu, YlOrBr, Greens, BuGn, Accents, GnBu, PuRd, Purples, RdYlGn, Paired, Blues, RdBu, Oranges, RdYlBu, PuBu, OrRd, Set3, Set2, Set1, Reds, PiYG, Dark2, YlGn, BrBG, YlGnBu, Pastel2, Pastel1
		BarVisualiser bar4 = new BarVisualiser(barDataset4, "Arc CO2 Emission from Car Trips [kt]", "Set2", false);
		bar4.setSize(600, 400);
		bar4.setVisible(true);

		ChartPanel cp = (ChartPanel) bar4.getContentPane().getComponent(0);
		JFreeChart chart = cp.getChart();
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);
		
		chart.getLegend().setHorizontalAlignment(HorizontalAlignment.CENTER);
		chart.getLegend().setVerticalAlignment(VerticalAlignment.CENTER);

		//uncomment to show value labels
		/*
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
	 	BarRenderer renderer = (BarRenderer)plot.getRenderer();
	 	renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
	 	renderer.setDefaultItemLabelsVisible(true);
	 	*/
	}
	
	private static void visualiseCarEnergyConsumptions (String baseline_2015, String baseline_2030, String scenario0_2030, String scenario1_2030,
			String scenario2_2030, String scenario3_2030, String scenario4_2030, String baseline_2050,
			String scenario0_2050, String scenario1_2050, String scenario2_2050, String scenario3_2050, String scenario4_2050) throws FileNotFoundException, IOException {

		DefaultCategoryDataset barDataset4 = new DefaultCategoryDataset();

		HashMap<Integer, HashMap<EnergyType, HashMap<String, Double>>> map = InputFileReader.readZonalCarEnergyConsumptionsFile(baseline_2015);
		HashMap<EnergyType, HashMap<String, Double>> energyMap = map.get(2015);
		double sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		double consumption = sum * 365 / 1000000;

		barDataset4.addValue(consumption, "baseline", "2015");
		//barDataset4.addValue(emission, "scenario0", "2015");
		//barDataset4.addValue(emission, "scenario1", "2015");
		//barDataset4.addValue(emission, "scenario2", "2015");
		//barDataset4.addValue(emission, "scenario3", "2015");
		//barDataset4.addValue(emission, "scenario4", "2015");


		map = InputFileReader.readZonalCarEnergyConsumptionsFile(baseline_2030);
		energyMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "baseline", "2030");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario0_2030);
		energyMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario0", "2030");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario1_2030);
		energyMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario1", "2030");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario2_2030);
		energyMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario2", "2030");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario3_2030);
		energyMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario3", "2030");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario4_2030);
		energyMap = map.get(2030);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario4", "2030");


		map = InputFileReader.readZonalCarEnergyConsumptionsFile(baseline_2050);
		energyMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "baseline", "2050");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario0_2050);
		energyMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario0", "2050");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario1_2050);
		energyMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario1", "2050");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario2_2050);
		energyMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario2", "2050");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario3_2050);
		energyMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario3", "2050");

		map = InputFileReader.readZonalCarEnergyConsumptionsFile(scenario4_2050);
		energyMap = map.get(2050);
		sum = 0.0;
		for (String zone: arcZones) 
			sum += energyMap.get(EnergyType.ELECTRICITY).get(zone);
		consumption = sum * 365 / 1000000;
		System.out.printf("Electricity consumption = %.2f MWh", consumption);
		barDataset4.addValue(consumption, "scenario4", "2050");



		//YlOrRd, PRGn, PuOr, RdGy, Spectral, Grays, PuBuGn, RdPu, BuPu, YlOrBr, Greens, BuGn, Accents, GnBu, PuRd, Purples, RdYlGn, Paired, Blues, RdBu, Oranges, RdYlBu, PuBu, OrRd, Set3, Set2, Set1, Reds, PiYG, Dark2, YlGn, BrBG, YlGnBu, Pastel2, Pastel1
		BarVisualiser bar4 = new BarVisualiser(barDataset4, "Arc Electricity Consumption for Car Trips [GWh]", "Set2", false);
		bar4.setSize(600, 400);
		bar4.setVisible(true);

		ChartPanel cp = (ChartPanel) bar4.getContentPane().getComponent(0);
		JFreeChart chart = cp.getChart();
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);

	}

	private static void visualiseTravelTimes(String baseline, String scenario0, String scenario1, String scenario2, String scenario3, String scenario4, int year) throws FileNotFoundException, IOException {

		//choose year and choose route (e.g. one graph is 2030, B1, 6 scenarios)
		
		SkimMatrix tsmB = new SkimMatrixMultiKey(baseline, null);
		SkimMatrix tsmS0 = new SkimMatrixMultiKey(scenario0, null);
		SkimMatrix tsmS1 = new SkimMatrixMultiKey(scenario1, null);
		SkimMatrix tsmS2 = new SkimMatrixMultiKey(scenario2, null);
		SkimMatrix tsmS3 = new SkimMatrixMultiKey(scenario3, null);
		SkimMatrix tsmS4 = new SkimMatrixMultiKey(scenario4, null);

		DefaultCategoryDataset barDataset1 = new DefaultCategoryDataset();

		//OX<->MK (baseline)
		double ox2mk = tsmB.getCost("E07000178", "E06000042");
		double mk2ox = tsmB.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (NO): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (NO): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (NO): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "baseline", "OX ↔ MK");

		//OX<->MK (scenario0)
		ox2mk = tsmS0.getCost("E07000178", "E06000042");
		mk2ox = tsmS0.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "scenario0", "OX ↔ MK");

		//OX<->MK (scenario1)
		ox2mk = tsmS1.getCost("E07000178", "E06000042");
		mk2ox = tsmS1.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "scenario1", "OX ↔ MK");
		
		//OX<->MK (scenario2)
		ox2mk = tsmS2.getCost("E07000178", "E06000042");
		mk2ox = tsmS2.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "scenario2", "OX ↔ MK");
		
		//OX<->MK (scenario3)
		ox2mk = tsmS3.getCost("E07000178", "E06000042");
		mk2ox = tsmS3.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "scenario3", "OX ↔ MK");
		
		//OX<->MK (scenario4)
		ox2mk = tsmS4.getCost("E07000178", "E06000042");
		mk2ox = tsmS4.getCost("E06000042", "E07000178");
		System.out.printf("Oxford to Milton Keynes (B1): %.2f %n", ox2mk);
		System.out.printf("Milton Keynes to Oxford (B1): %.2f %n", mk2ox);
		System.out.printf("Average between Oxford and Milton Keynes (B1): %.2f %n", (ox2mk + mk2ox) / 2);
		barDataset1.addValue((ox2mk + mk2ox) / 2, "scenario4", "OX ↔ MK");

		//------------------------------------------------------------------------------------------------

		//MK<->CAM (baseline)
		double mk2cam = tsmB.getCost("E06000042", "E07000008");
		double cam2mk = tsmB.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (NO): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (NO): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (NO): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "baseline", "MK ↔ CAM");

		//MK<->CAM (scenario0)
		mk2cam = tsmS0.getCost("E06000042", "E07000008");
		cam2mk = tsmS0.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "scenario0", "MK ↔ CAM");
		
		//MK<->CAM (scenario1)
		mk2cam = tsmS1.getCost("E06000042", "E07000008");
		cam2mk = tsmS1.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "scenario1", "MK ↔ CAM");
		
		//MK<->CAM (scenario2)
		mk2cam = tsmS2.getCost("E06000042", "E07000008");
		cam2mk = tsmS2.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "scenario2", "MK ↔ CAM");
		
		//MK<->CAM (scenario3)
		mk2cam = tsmS3.getCost("E06000042", "E07000008");
		cam2mk = tsmS3.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "scenario3", "MK ↔ CAM");
		
		//MK<->CAM (scenario4)
		mk2cam = tsmS4.getCost("E06000042", "E07000008");
		cam2mk = tsmS4.getCost("E07000008", "E06000042");
		System.out.printf("Milton Keynes to Cambridge (B1): %.2f %n", mk2cam);
		System.out.printf("Cambridge to Milton Keynes (B1): %.2f %n", cam2mk);
		System.out.printf("Average between Milton Keynes and Cambridge (B1): %.2f %n", (mk2cam + cam2mk) / 2);
		barDataset1.addValue((mk2cam + cam2mk) / 2, "scenario4", "MK ↔ CAM");

		//------------------------------------------------------------------------------------------------

		//OX<->CAM (baseline)
		double ox2cam = tsmB.getCost("E07000178", "E07000008");
		double cam2ox = tsmB.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (NO): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (NO): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (NO): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "baseline", "OX ↔ CAM");

		//OX<->CAM (scenario0)
		ox2cam = tsmS0.getCost("E07000178", "E07000008");
		cam2ox = tsmS0.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "scenario0", "OX ↔ CAM");

		//OX<->CAM (scenario1)
		ox2cam = tsmS1.getCost("E07000178", "E07000008");
		cam2ox = tsmS1.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "scenario1", "OX ↔ CAM");
		
		//OX<->CAM (scenario2)
		ox2cam = tsmS2.getCost("E07000178", "E07000008");
		cam2ox = tsmS2.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "scenario2", "OX ↔ CAM");
		
		//OX<->CAM (scenario3)
		ox2cam = tsmS3.getCost("E07000178", "E07000008");
		cam2ox = tsmS3.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "scenario3", "OX ↔ CAM");
		
		//OX<->CAM (scenario4)
		ox2cam = tsmS4.getCost("E07000178", "E07000008");
		cam2ox = tsmS4.getCost("E07000008", "E07000178");
		System.out.printf("Oxford to Cambridge (B1): %.2f %n", ox2cam);
		System.out.printf("Cambridge to Oxford (B1): %.2f %n", cam2ox);
		System.out.printf("Average between Oxford and Cambridge (B1): %.2f %n", (ox2cam + cam2ox) / 2);
		barDataset1.addValue((ox2cam + cam2ox) / 2, "scenario4", "OX ↔ CAM");

		//String title = scenario + " (" + year + ")";
		String title = "Intercity Travel Times (" + year + ") [min]";
		BarVisualiser bar1 = new BarVisualiser(barDataset1, title, "Set2", false);
		bar1.setSize(600, 400);
		bar1.setVisible(true);
		//line.saveToPNG("BarVisualiserTest2.png");

		ChartPanel cp = (ChartPanel) bar1.getContentPane().getComponent(0);
		JFreeChart chart = cp.getChart();
		Font titleFont = new Font("Calibri", Font.BOLD, 22);
		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		chart.getTitle().setFont(titleFont);

		Font font3 = new Font("Calibri", Font.PLAIN, 40); 
		chart.getCategoryPlot().getDomainAxis().setLabelFont(font3);
		chart.getCategoryPlot().getRangeAxis().setLabelFont(font3);
	}
}

