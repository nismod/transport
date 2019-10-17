package nismod.transport.scripts;

public class RunArcRail {

	public static void main(String[] args) {
		
		String configFile = "./src/main/full/config/configNObaseline.properties";
		String[] arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);	
		
		configFile = new String("./src/main/full/config/configB1baseline.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configB3baseline.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configNOscenario1.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configB1scenario1.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configB3scenario1.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configNOscenario2.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configB1scenario2.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
		
		configFile = new String("./src/main/full/config/configB3scenario2.properties");
		arguments = new String[] {"-c", configFile, "-rail", "2030", "2015"};
		nismod.transport.App.main(arguments);
		arguments = new String[] {"-c", configFile, "-rail", "2050", "2030"};
		nismod.transport.App.main(arguments);
	}
}
