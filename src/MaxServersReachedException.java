/**
 * Distributed Computing Spring '11 HW3 Project
 *  https://github.com/jonasrmichel/TixReservation
 * 
 * @author Kyle Prete
 * @date Feb 24, 2011
 * 
 * This file contains a custom exception class.
 */

@SuppressWarnings("serial")
public class MaxServersReachedException extends Exception {

	public MaxServersReachedException(String string) {
		super(string);
	}

}
