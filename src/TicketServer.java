import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class TicketServer {
	SeatTable seatTable_;
	static int myID;
	int myClock;
	static List<Socket> otherActiveServers = new ArrayList<Socket>();

	// TODO: lamport mutex "queue" data structure (e.g., hashmap?)

	public TicketServer() {
		myClock = 0;
		seatTable_ = new SeatTable();
		Symbols.initServerLists();
	}

	static ServerSocket getUnusedPort(List<Integer> portList) throws MaxServersReachedException {
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
					throw new MaxServersReachedException("Max servers reached");
				}
			}
		}
		return socket;
	}
	
	public void doStuff() throws MaxServersReachedException {
		ServerSocket listener = getUnusedPort(Symbols.serverList_Public);
		myID = listener.getLocalPort();
		new Thread(new ClientHandlerRunner(listener, this.seatTable_))
				.start();
		ServerSocket serverListener = getUnusedPort(Symbols.serverList_Private);
		for (int i : Symbols.serverList_Private) {
			if (i == serverListener.getLocalPort())
				continue;

			try {
				Socket server = new Socket(Symbols.ticketServer, i);
				otherActiveServers.add(server);
			} catch (IOException ex) {
				// Server is not up... ignore.
			}
		}

		while (true) {
			try {
				Socket anotherServer = serverListener.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Do some stuff with it.
		}

		
	}

	public static void main(String[] args) {
		try {
			TicketServer tixServer = new TicketServer();
			tixServer.doStuff();
		} catch (MaxServersReachedException e) {
			System.err.println("Server aborted: " + e);
		}
	}
	
	public class ClientHandlerRunner implements Runnable {

		protected ServerSocket serverSocket_ = null;

		public ClientHandlerRunner(ServerSocket serverSocket, SeatTable seatTable) {
			this.serverSocket_ = serverSocket;
		}

		@Override
		public void run() {

			try {
				while (true) {
					Socket clientSocket = serverSocket_.accept();
					BufferedReader din = new BufferedReader(new InputStreamReader(
							clientSocket.getInputStream()));
					PrintWriter pout = new PrintWriter(
							clientSocket.getOutputStream());
					String getline = din.readLine();
					StringTokenizer st = new StringTokenizer(getline);

					String rmi = st.nextToken();
					String name = st.nextToken();

					System.out.println("Request: " + rmi + " " + name);

					int index = -3; // initialize to unused val
					// TODO: mutex
					if (rmi.equals("reserve")) {
						getMutex();
						index = seatTable_.reserve(name);
						releaseMutex();
					} else if (rmi.equals("search")) {
						index = seatTable_.search(name);
					} else if (rmi.equals("delete")) {
						getMutex();
						index = seatTable_.delete(name);
						releaseMutex();
					}
					pout.println(index);
					pout.flush();
				}
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		private void releaseMutex() {
			// TODO Auto-generated method stub
			
		}

		private void getMutex() {
			// TODO Auto-generated method stub
			
		}

	}
}
