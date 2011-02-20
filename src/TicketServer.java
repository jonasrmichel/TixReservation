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
	volatile long[] myClock = new long[Symbols.maxServers];
	volatile boolean wantMutex;
	static List<Socket> otherActiveServers = new ArrayList<Socket>();

	// TODO: lamport mutex "queue" data structure (e.g., hashmap?)

	public TicketServer() {
		seatTable_ = new SeatTable();
		for (int i = 0; i < Symbols.maxServers; ++i) {
			myClock[i] = 0;
		}
		Symbols.initServerLists();
	}

	static ServerSocket getUnusedPort(List<Integer> portList)
			throws MaxServersReachedException {
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
		ServerSocket serverListener = getUnusedPort(Symbols.serverList_Private);
		myID = serverListener.getLocalPort()-Symbols.basePort_Private;
		myClock[myID] = 1;
		new Thread(new ClientHandlerRunner(listener)).start();
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
		while(true) {
			try {
				Socket anotherServer = serverListener.accept();
				new Thread(new ServerHandlerRunner(anotherServer))
					.start();
			} catch (IOException ex){
				System.err.println(ex);
			}
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

	public class ServerHandlerRunner implements Runnable {

		protected Socket clientSocket = null;

		public ServerHandlerRunner(Socket serverSocket) {
			this.clientSocket = serverSocket;
		}

		@Override
		public void run() {
			try {
				BufferedReader din = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				PrintWriter pout = new PrintWriter(
						clientSocket.getOutputStream());
				String getline = din.readLine();
				StringTokenizer st = new StringTokenizer(getline);

				String rmi = st.nextToken(); // req, rel
				long newClock = Long.parseLong(st.nextToken()); // clock val

				if (rmi.equals("req")) { // received a request


				} else if (rmi.equals("rel")) { // received a release
					String mod = st.nextToken(); // reserve, delete
					String name = st.nextToken();

					if (mod.equals("res")) {
						seatTable_.reserve(name);
					} else if (mod.equals("del")) {
						seatTable_.delete(name);
					}
				} else if (rmi.equals("hey")) { // new server

				}
				// TODO: update clock vector
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}

	public class ClientHandlerRunner implements Runnable {

		protected ServerSocket serverSocket_ = null;

		public ClientHandlerRunner(ServerSocket serverSocket) {
			this.serverSocket_ = serverSocket;
		}

		@Override
		public void run() {
			try {
				while (true) {
					Socket clientSocket = serverSocket_.accept();
					BufferedReader din = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
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
						if (index < 0)
							releaseMutex("null", "");
						else
							releaseMutex("res", name);
					} else if (rmi.equals("search")) {
						index = seatTable_.search(name);
					} else if (rmi.equals("delete")) {
						getMutex();
						index = seatTable_.delete(name);
						if (index < 0)
							releaseMutex("null", "");
						else
							releaseMutex("del", name);
					}
					pout.println(index);
					pout.flush();
				}
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		private void releaseMutex(String command, String name) {
			wantMutex = false;
			for (Socket s : otherActiveServers) {
				try {
					PrintWriter pout = new PrintWriter(s.getOutputStream());
					pout.println("rel " + myClock + " " + command + " " + name);
					pout.flush();
				} catch (IOException e) {
					// Server is dead.
					otherActiveServers.remove(s);
				}
			}
			++myClock[myID];
		}

		private void getMutex() {
			wantMutex = true;
			for (Socket s : otherActiveServers) {
				try {
					PrintWriter pout = new PrintWriter(s.getOutputStream());
					pout.println("req " + myClock);
					pout.flush();
				} catch (IOException e) {
					// Server is dead.
					otherActiveServers.remove(s);
				}
			}
			++myClock[myID];
			for (Socket s : otherActiveServers) {
				try {
					BufferedReader din = new BufferedReader(
							new InputStreamReader(s.getInputStream()));
					String ok = din.readLine();

					StringTokenizer st = new StringTokenizer(ok);
					String token = st.nextToken();
					assert token.equals("ack");
					token = st.nextToken();
					myClock[myID] = Math.max(myClock[myID], Long.parseLong(token)) + 1;
					int theirID = s.getPort()-Symbols.basePort_Private;
					myClock[theirID] = Math.max(myClock[theirID], Long.parseLong(token));
				} catch (IOException e) {
					// Server is dead.
					otherActiveServers.remove(s);
				}
			}
		}
	}
}
