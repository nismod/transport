package nismod.transport.utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

	public ConfigReader() {
		// TODO Auto-generated constructor stub
	}

	public static Properties getProperties(String configFile) {
		
	
		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(configFile);
			// load properties file
			props.load(inputStream);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.err.println("Unable to load config.properties file!");
			
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return props;
		
	}
}
