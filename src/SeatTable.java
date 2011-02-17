
public class SeatTable {
	final int maxSize = 100;
	private String[] seats = new String[maxSize];

	/**
	 *
	 * @param name
	 * @return seat number assigned
	 * 		-1 reservation already exists for name
	 * 		-2 list full
	 */
	int reserve(String name) {
		if (search(name) == -1) {
			int seatNum = bestSeat();
			if (seatNum != -2) {
				seats[seatNum] = name;
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
	int delete(String name) {
		int seatNum = search(name);
		if (seatNum != -1) {
			seats[seatNum] = null;
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

