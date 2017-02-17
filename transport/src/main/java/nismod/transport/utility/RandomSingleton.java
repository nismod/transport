/**
 * 
 */
package nismod.transport.utility;

import java.util.Random;

/**
 * @author Milan Lovric
 *
 */
public class RandomSingleton {
	
	private static RandomSingleton instance;
	private Random generator;
	
	private RandomSingleton() {
		
		generator = new Random(1234);
	}
	
	public static RandomSingleton getInstance () {
		
		if (instance == null) instance = new RandomSingleton();
		
		return instance;
	}
	
	public double nextDouble() { 
	
		return generator.nextDouble(); 
	} 
	
	public int nextInt(int bound) { 
		
		return generator.nextInt(bound); 
	} 
}
