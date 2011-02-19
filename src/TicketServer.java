import java.net.*;
import java.io.*;
import java.util.*;

public class TicketServer {
	SeatTable table;
	static int myID;
	int myClock;
	// TODO: lamport mutex "queue" data structure (e.g., hashmap?)

	public TicketServer() {
		myClock = 0;
		table = new SeatTable();
		Symbols.initServerLists();
	}

	static ServerSocket getUnusedPort(List<Integer> portList) throws Exception {
		ServerSocket socket = null;
		int tryPort = 0;
		for (int i = 0; i < portList.size(); i++) {
			try {
				tryPort = portList.get(i);
				System.out.print("Trying port " + tryPort + "...");
				socket = new ServerSocket(tryPort);
				System.out.println("SUCCESS");
				return socket;
			} catch (Exception e) {
				System.out.println("IN USE");
				if (i == portList.size() - 1) {
					throw new Exception("Max servers reached");
				}
			}
		}
		return socket;
	}

	void handleClient(Socket theClient) {
		try {
			BufferedReader din = new BufferedReader
				(new InputStreamReader(theClient.getInputStream()));
			PrintWriter pout = new PrintWriter(theClient.getOutputStream());
			String getline = din.readLine();
			StringTokenizer st = new StringTokenizer(getline);

			String tag = st.nextToken();

			if (tag.equals(Symbols.clientTag)) { // handle client request
				String rmi = st.nextToken();
				String name = st.nextToken();

				System.out.println("Request: " + rmi + " " + name);

				int index = -3; // initialize to unused val
				if (rmi.equals("reserve")) {
					index = table.reserve(name);
				} else if (rmi.equals("search")) {
					index = table.search(name);
				} else if (rmi.equals("delete")) {
					index = table.delete(name);
				}
				pout.println(index);
				pout.flush();
			} else if (tag.equals(Symbols.serverTag)) { // handle server request

			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	// TODO: class Listener implements Runnable

	public static void main(String[] args) {
		try {
			TicketServer tixServer = new TicketServer();
			ServerSocket listener = getUnusedPort(Symbols.serverList_Public);
			myID = listener.getLocalPort();
			while(true) {
				Socket aClient = listener.accept();
				tixServer.handleClient(aClient);
				aClient.close();
			}
		} catch (Exception e) {
			System.err.println("Server aborted: " + e);
		}
	}
}
