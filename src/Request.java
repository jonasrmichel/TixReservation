
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
