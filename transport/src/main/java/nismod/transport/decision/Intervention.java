/**
 * 
 */
package nismod.transport.decision;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Milan Lovric
 *
 */
public abstract class Intervention {
	
	protected Properties props;
	protected boolean installed;
	
	protected Intervention (Properties props) {
		
		this.props = props;
		
		this.installed = false;
	}
	
	protected Intervention (String fileName) {
		
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(fileName);
			// load properties file
			props.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		this.props = props;
		
		this.installed = false;
	}
	
	public abstract void install(Object o);
	
	public abstract void uninstall(Object o);
	
	/**
	 * @return The year in which intervention is installed.
	 */
	public int getStartYear() {
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		return startYear;
	}
	
	/**
	 * @return The last year in which intervention still remains installed.
	 */
	public int getEndYear() {
		
		int endYear = Integer.parseInt(props.getProperty("endYear"));
		return endYear;
	}
	
	public boolean getState() {
		
		return installed;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		return props.toString();
	}
}
