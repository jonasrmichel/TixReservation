import java.util.ArrayList;
import java.util.List;

public class Symbols {
	final static int serverPort = 1234;
	static final int TIMEOUT_ = 3000;
	final static String ticketServer = "localhost";
	static List<Integer> serverList_Public = new ArrayList<Integer>();
	static List<Integer> serverList_Private = new ArrayList<Integer>();
	final static int basePort_Public = 1234;
	final static int basePort_Private = 2234;
	final static int maxServers = 5;

	static void initServerLists() {
		if (serverList_Private.isEmpty()) {
			for (int i = 0; i < maxServers; i++) {
				serverList_Public.add(basePort_Public + i);
				serverList_Private.add(basePort_Private + i);
			}
		}
	}
}
