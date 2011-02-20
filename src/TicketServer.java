import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

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

	public static void main(String[] args) {
		try {
			TicketServer tixServer = new TicketServer();
			ServerSocket listener = getUnusedPort(Symbols.serverList_Public);
			myID = listener.getLocalPort();
			new Thread(new ClientHandlerRunner(listener, tixServer.table))
					.start();
			ServerSocket serverListener = getUnusedPort(Symbols.serverList_Private);
			while (true) {
				Socket anotherServer = serverListener.accept();
				// Do some stuff with it.
			}

		} catch (Exception e) {
			System.err.println("Server aborted: " + e);
		}
	}
}
