/**
 * 
 */
package nismod.transport.decision;

import java.util.Properties;

import nismod.transport.demand.DemandModel;

/**
 * @author Milan Lovric
 *
 */
public abstract class Intervention {
	
	protected Properties props;
	protected boolean installed;
	private int startYear;
	private int endYear;
	
	protected Intervention (int startYear, int endYear, Properties props) {
		
		this.startYear = startYear;
		this.endYear = endYear;
		this.props = props;
	}
	
	public abstract void install(Object o);
	
	public abstract void uninstall(Object o);
	
	public int getStartYear() {
		
		return startYear;
	}
	
	public int getEndYear() {

		return endYear;
	}
	
	public boolean getState() {
		
		return installed;
	}
}
