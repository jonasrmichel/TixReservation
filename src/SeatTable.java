/**
 * Distributed Computing Spring '11 HW3 Project
 *  https://github.com/jonasrmichel/TixReservation
 * 
 * @author Jonas Michel
 * @date Feb 24, 2011
 * 
 * This file holds the seating chart. It is shared among the
 * threads of a particular server, but each server has its
 * own instance.
 */

public class SeatTable {
	final int maxSize = 100;
	public String[] seats = new String[maxSize];
	private int count = 0;

	public int getCount() {
		return count;
	}

	public void setCount(int num) {
		count = num;
	}

	/**
	 *
	 * @param name
	 * @return seat number assigned
	 * 		-1 reservation already exists for name
	 * 		-2 list full
	 */
	synchronized int reserve(String name) {
		if (search(name) == -1) {
			int seatNum = bestSeat();
			if (seatNum != -2) {
				seats[seatNum] = name;
				++count;
			}
			return seatNum;
		} else {
			return -1;
		}
	}

	/**
	 *
	 * @param name
	 * @return seat number assigned
	 * 		-1 name not found
	 */
	int search(String name) {
		for (int i = 0; i < maxSize; i++) {
			if (!emptySeat(seats[i]) && seats[i].equals(name))
				return i;
		}
		return -1;
	}

	/**
	 *
	 * @param name
	 * @return seat number freed
	 * 		-1 name not found
	 */
	synchronized int delete(String name) {
		int seatNum = search(name);
		if (seatNum != -1) {
			seats[seatNum] = null;
			--count;
		}
		return seatNum;
	}

	/**
	 *
	 * @return index of most desirable seat (0 best, maxSize-1 worst)
	 * 		-2 full
	 */
	int bestSeat() {
		for( int i = 0; i < maxSize; i++) {
			if (emptySeat(seats[i])) return i;
		}
		return -2;
	}

	/**
	 *
	 * @param str
	 * @return str indicates empty seat
	 */
	boolean emptySeat(String str) {
		return str == null;
	}
}

