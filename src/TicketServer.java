import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

public class TicketServer {
	private static final int TIMEOUT_ = 3000;
	SeatTable seatTable_;
	static int myID;
	volatile long[] myClock = new long[Symbols.maxServers];
	static List<Integer> otherActiveServers = new ArrayList<Integer>();
	volatile PriorityQueue<Request> requests = new PriorityQueue<Request>(
			Symbols.maxServers);

	public TicketServer() {
		seatTable_ = new SeatTable();
		for (int i = 0; i < Symbols.maxServers; ++i) {
			myClock[i] = 0;
		}
		Symbols.initServerLists();
	}

	static ServerSocket getUnusedPort(List<Integer> portList)
			throws MaxServersReachedException {
		for (int i = 0; i < portList.size(); i++) {
			try {
				int tryPort = portList.get(i);
				System.out.print("Trying port " + tryPort + "...");
				ServerSocket socket = new ServerSocket(tryPort);
				System.out.println("SUCCESS");
				return socket;
			} catch (IOException e) {
				System.out.println("IN USE");
			}
		}
		// Have tried all ports...
		throw new MaxServersReachedException("Max servers reached");
	}

	public void doStuff() throws MaxServersReachedException {
		ServerSocket listener = getUnusedPort(Symbols.serverList_Public);
		ServerSocket serverListener = getUnusedPort(Symbols.serverList_Private);
		myID = serverListener.getLocalPort() - Symbols.basePort_Private;
		myClock[myID] = 1;
		for (int i : Symbols.serverList_Private) {
			if (i == serverListener.getLocalPort())
				continue;
			try {
				Socket server = new Socket(Symbols.ticketServer, i);
				server.setSoTimeout(TIMEOUT_);
				PrintStream pout = new PrintStream(server.getOutputStream());
				pout.println("hey " + myID + " " + 1);
				pout.flush();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						server.getInputStream()));
				String line = br.readLine();
				StringTokenizer st = new StringTokenizer(line);
				String rmi = st.nextToken(); // should be ack
				assert rmi.equals("ack");
				long theirClock = Long.parseLong(st.nextToken()); // clock val
				int theirID = i - Symbols.basePort_Private;
				myClock[myID] = Math.max(myClock[myID], theirClock) + 1;
				myClock[theirID] = Math.max(myClock[theirID], theirClock);
				otherActiveServers.add(theirID);
			} catch (IOException ex) {
				// Server is not up... ignore.
			}
		}
		if (!otherActiveServers.isEmpty()) {
			getMutex();
			try {
				Socket firstguy = new Socket(Symbols.ticketServer,
						otherActiveServers.get(0) + Symbols.basePort_Private);
				firstguy.setSoTimeout(TIMEOUT_);
				PrintStream pout = new PrintStream(firstguy.getOutputStream());
				pout.println("gst " + myID + " " + myClock[myID]);
				pout.flush();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						firstguy.getInputStream()));
				String line = br.readLine();
				StringTokenizer st = new StringTokenizer(line);
				String rmi = st.nextToken(); // should be rdy
				assert rmi.equals("rdy");
				int theirID = Integer.parseInt(st.nextToken());
				long theirClock = Long.parseLong(st.nextToken());
				myClock[myID] = Math.max(myClock[myID], theirClock) + 1;
				myClock[theirID] = Math.max(myClock[theirID], theirClock);
				int numInTable = Integer.parseInt(st.nextToken());
				for (int i = 0; i < numInTable; ++i) {
					String seatEntry = br.readLine();
					st = new StringTokenizer(seatEntry);
					int seat = Integer.parseInt(st.nextToken());
					String name = st.nextToken();
					seatTable_.seats[seat] = name;
				}
			} catch (IOException ex) {
				// This shouldn't happen?
				assert false;
			}
			releaseMutex("null", "null");

		}
		new Thread(new ClientHandlerRunner(listener)).start();
		while (true) {
			try {
				Socket anotherServer = serverListener.accept();
				new Thread(new ServerHandlerRunner(anotherServer)).start();
			} catch (IOException ex) {
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

	private void releaseMutex(String command, String name) {
		List<Integer> temp = new ArrayList<Integer>();
		for (int server : otherActiveServers) {
			try {
				Socket s = new Socket(Symbols.ticketServer, server
						+ Symbols.basePort_Private);
				s.setSoTimeout(TIMEOUT_);
				PrintWriter pout = new PrintWriter(s.getOutputStream());
				pout.println("rel " + myID + " " + myClock[myID] + " "
						+ command + " " + name);
				pout.flush();
			} catch (IOException e) {
				// Server is dead.
				temp.add(server);
			}
		}
		otherActiveServers.removeAll(temp);
		++myClock[myID];
		synchronized (requests) {
			requests.remove();
			requests.notifyAll();
		}
	}

	private void getMutex() {
		List<Integer> temp = new ArrayList<Integer>();
		synchronized (requests) {
			requests.add(new Request(myID, myClock[myID]));
		}
		for (int server : otherActiveServers) {
			try {
				Socket s = new Socket(Symbols.ticketServer, server
						+ Symbols.basePort_Private);
				s.setSoTimeout(TIMEOUT_);
				PrintWriter pout = new PrintWriter(s.getOutputStream());
				pout.println("req " + myID + " " + myClock[myID]);
				pout.flush();
				BufferedReader din = new BufferedReader(new InputStreamReader(
						s.getInputStream()));
				String ok = din.readLine();

				StringTokenizer st = new StringTokenizer(ok);
				String token = st.nextToken();
				assert token.equals("ack");
				int theirID = Integer.parseInt(st.nextToken());
				token = st.nextToken();
				myClock[myID] = Math.max(myClock[myID], Long.parseLong(token)) + 1;
				myClock[theirID] = Math.max(myClock[theirID],
						Long.parseLong(token));
			} catch (IOException e) {
				// Server is dead.
				temp.add(server);
			}
		}
		++myClock[myID];
		otherActiveServers.removeAll(temp);
		synchronized (requests) {
			while (requests.peek().id_ != myID) {
				try {
					requests.wait();

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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

				String rmi = st.nextToken(); // req, rel, etc
				int theirID = Integer.parseInt(st.nextToken());
				long theirClock = Long.parseLong(st.nextToken()); // clock val

				if (rmi.equals("req")) { // received a request
					synchronized (requests) {
						requests.add(new Request(theirID, theirClock));
					}
					pout.println("ack " + myID + " " + myClock[myID]);
					pout.flush();
				} else if (rmi.equals("rel")) { // received a release
					String mod = st.nextToken(); // reserve, delete
					String name = st.nextToken();

					synchronized (requests) {
						requests.remove();
						requests.notifyAll();
					}

					if (mod.equals("res")) {
						seatTable_.reserve(name);
					} else if (mod.equals("del")) {
						seatTable_.delete(name);
					}
				} else if (rmi.equals("hey")) { // new server
					otherActiveServers.add(theirID);
					pout.println("ack " + myID + " " + myClock[myID]);
					pout.flush();
				} else if (rmi.equals("gst")) {
					pout.println("rdy " + myID + " " + myClock[myID] + " "
							+ seatTable_.getCount());
					pout.flush();
					int sent = 0;
					for (int i = 0; i < seatTable_.maxSize
							&& sent < seatTable_.getCount(); ++i) {
						if (seatTable_.emptySeat(seatTable_.seats[i]))
							continue;
						pout.println(i + " " + seatTable_.seats[i]);
						pout.flush();
						++sent;
					}
				}
				myClock[myID] = Math.max(myClock[myID], theirClock) + 1;
				myClock[theirID] = Math.max(myClock[theirID], theirClock);
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
							releaseMutex("null", "null");
						else
							releaseMutex("res", name);
					} else if (rmi.equals("search")) {
						index = seatTable_.search(name);
					} else if (rmi.equals("delete")) {
						getMutex();
						index = seatTable_.delete(name);
						if (index < 0)
							releaseMutex("null", "null");
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

	}
}
