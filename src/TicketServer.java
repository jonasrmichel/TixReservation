import java.net.*;
import java.io.*;
import java.util.*;

public class TicketServer {
	SeatTable table;

	public TicketServer() {
		table = new SeatTable();
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
		TicketServer tixServer = new TicketServer();
		try {
			ServerSocket listner = new ServerSocket(Symbols.serverPort);
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
