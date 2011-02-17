import java.net.*;
import java.io.*;
import java.util.*;

public class TicketServer {
	SeatTable table;

	public TicketServer() {
		table = new SeatTable();
		Symbols.initServerList();
	}

	static ServerSocket getUnusedPort() throws Exception {
		ServerSocket socket = null;
		int tryPort = 0;
		for (int i = 0; i < Symbols.serverList.size(); i++) {
			try {
				tryPort = Symbols.serverList.get(i);
				System.out.print("Trying port " + tryPort + "...");
				socket = new ServerSocket(tryPort);
				System.out.println("SUCCESS");
				return socket;
			} catch (Exception e) {
				System.out.println("IN USE");
				if (i == Symbols.serverList.size() - 1) {
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

			// handle client request
			String name = st.nextToken();

			System.out.println("Request: " + tag + " " + name);

			int index = -3; // initialize to unused val
			if (tag.equals("reserve")) {
				index = table.reserve(name);
			} else if (tag.equals("search")) {
				index = table.search(name);
			} else if (tag.equals("delete")) {
				index = table.delete(name);
			}
			pout.println(index);
			pout.flush();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static void main(String[] args) {
		try {
			TicketServer tixServer = new TicketServer();
			ServerSocket listner = getUnusedPort();
			while(true) {
				Socket aClient = listner.accept();
				tixServer.handleClient(aClient);
				aClient.close();
			}
		} catch (Exception e) {
			System.err.println("Server aborted: " + e);
		}
	}
}
