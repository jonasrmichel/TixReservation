/**
 * Distributed Computing Spring '11 HW3 Project
 *  https://github.com/jonasrmichel/TixReservation
 * 
 * @author Kyle Prete
 * @date Feb 24, 2011
 * 
 * This file contains a custom Request object definition.
 * We use this along with a priority queue to ensure the
 * oldest request is the next to receive the mutex. A tie
 * is broken by server id.
 */

public class Request implements Comparable<Request> {
	int id_;
	long clock_;

	public Request(int id, long clock) {
		id_ = id;
		clock_ = clock;
	}

	@Override
	public String toString() {
		return "processor: " + id_ + " clock: " + clock_;
	}

	@Override
	public int compareTo(Request other) {
		if (this.clock_ < other.clock_)
			return -1;
		if (this.clock_ > other.clock_)
			return 1;
		if (this.id_ < other.id_)
			return -1;
		return 1;
	}

}
