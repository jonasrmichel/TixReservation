import java.util.*;

public class Symbols {
	final static int serverPort = 1234;
	final static String ticketServer = "localhost";
	static List<Integer> serverList = new ArrayList<Integer>();
	final static int basePort = 1234;
	final static int maxServers = 5;

	static void initServerList() {
		for (int i = 0; i < maxServers; i++) {
			serverList.add(basePort + i);
		}
	}

}
