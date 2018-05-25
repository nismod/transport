/**
 * 
 */
package nismod.transport.utility;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates only one instance of the random number generator that can be used throughout the whole model.
 * Simulation results can then be reproduced by using the same seed.
 * @author Milan Lovric
 *
 */
public class RandomSingleton {
	
	private final static Logger LOGGER = LogManager.getLogger(RandomSingleton.class);
	
	private static RandomSingleton instance;
	private Random generator;
	
	private RandomSingleton() {
		
		generator = new Random(1234);
	}
	
	/**
	 * Getter for the singleton instance of the random number generator.
	 * @return Random number generator.
	 */
	public static RandomSingleton getInstance() {
		
		if (instance == null) instance = new RandomSingleton();
		
		return instance;
	}
	
	/**
	 * Generates a pseudorandom real number between 0 and 1.
	 * @return Pseudorandom real double.
	 */
	public double nextDouble() { 
	
		return generator.nextDouble(); 
	} 
	
	/**
	 * Generates a pseudorandom whole number smallet than the bound.
	 * @param bound Upper bound.
	 * @return Pseudorandom whole number.
	 */
	public int nextInt(int bound) { 
		
		return generator.nextInt(bound); 
	}
	
	/**
	 * Setter method for the seed of the random number generator.
	 * @param seed Seed of the random number generator.
	 */
	public void setSeed(long seed) {
		
		generator = new Random(seed);
	}
}
