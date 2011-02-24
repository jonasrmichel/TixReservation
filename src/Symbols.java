/**
 * Distributed Computing Spring '11 HW3 Project
 *  https://github.com/jonasrmichel/TixReservation
 *
 * @author Jonas Michel
 * @date Feb 24, 2011
 *
 * This file contains constants and global variables.
 */

import java.util.ArrayList;
import java.util.List;

public class Symbols {
	final static int maxServers = 10;
	final static int maxTestClients = 25;
	static final int timeout = 3000;
	final static String ticketServer = "localhost";
	static List<Integer> serverList_Public = new ArrayList<Integer>();
	static List<Integer> serverList_Private = new ArrayList<Integer>();
	final static int basePort_Public = 1234;
	final static int basePort_Private = 2234;
	final static int invalid = -3;

	static void initServerLists() {
		if (serverList_Private.isEmpty()) {
			for (int i = 0; i < maxServers; i++) {
				serverList_Public.add(basePort_Public + i);
				serverList_Private.add(basePort_Private + i);
			}
		}
	}
}
