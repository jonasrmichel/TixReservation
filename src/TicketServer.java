import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

public class TicketServer {
	SeatTable seatTable_;
	static int myID;
	volatile long[] myClock = new long[Symbols.maxServers];
	private static List<Socket> otherActiveServers = new ArrayList<Socket>();
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
				server.setSoTimeout(Symbols.timeout);
				PrintWriter pout = new PrintWriter(server.getOutputStream());
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
				otherActiveServers.add(server);
			} catch (IOException ex) {
				// Server is not up... ignore.
			}
		}
		if (!otherActiveServers.isEmpty()) {
			getMutex();
			try {
				Socket firstguy = otherActiveServers.get(0);
				PrintWriter pout = new PrintWriter(firstguy.getOutputStream());
				pout.println("gst " + myID + " " + myClock[myID]);
				pout.flush();
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
		System.out.println("releasing mutex!");
		List<Socket> temp = new ArrayList<Socket>();
		for (Socket s : otherActiveServers) {
			try {
				PrintWriter pout = new PrintWriter(s.getOutputStream());
				pout.println("rel " + myID + " " + myClock[myID] + " " + command + " " + name);
				pout.flush();
			} catch (IOException e) {
				// Server is dead.
				temp.add(s);
			}
		}
		otherActiveServers.removeAll(temp);
		++myClock[myID];
		synchronized (requests) {
			requests.remove();
			requests.notifyAll();
		}
		System.out.println("released mutex!");
	}

	private void getMutex() {
		System.out.println("getting mutex!");
		List<Socket> temp = new ArrayList<Socket>();
		synchronized (myClock) {
			for (Socket s : otherActiveServers) {
				try {
					PrintWriter pout = new PrintWriter(s.getOutputStream());
					System.out.println("sending:" + "req " + myID + " " + myClock[myID]);
					pout.println("req " + myID + " " + myClock[myID]); // ** not received
					pout.flush();
				} catch (IOException e) {
					// Server is dead.
					temp.add(s);
				}
			}
			synchronized (requests) {
				System.out.println("adding myself to requests");
				requests.add(new Request(myID, myClock[myID]));
			}
		}
		++myClock[myID];
		int theirID = -1;
		System.out.println("checking for acks back");
		for (Socket s : otherActiveServers) {
			try {
				BufferedReader din = new BufferedReader(new InputStreamReader(s.getInputStream()));
				String ok = din.readLine();
				System.out.println(myClock[myID] + ":" + ok);
				StringTokenizer st = new StringTokenizer(ok);
				String token = st.nextToken();
				assert token.equals("ack");
				theirID = Integer.parseInt(st.nextToken());
				System.out.println("got ack from:" + theirID);
				token = st.nextToken();
				myClock[myID] = Math.max(myClock[myID], Long.parseLong(token)) + 1;
				myClock[theirID] = Math.max(myClock[theirID],
						Long.parseLong(token));
			} catch (IOException e) {
				// Server is dead.
				System.out.println("dead server");
				temp.add(s);
			}
		}
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
		System.out.println("got mutex!");
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
				System.out.println(myClock[myID] + ":" + getline);
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
					otherActiveServers.add(clientSocket);
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
				} else if (rmi.equals("rdy")) {
					int numInTable = Integer.parseInt(st.nextToken());
					seatTable_.setCount(numInTable);
					for (int i = 0; i < numInTable; ++i) {
						String seatEntry = din.readLine();
						st = new StringTokenizer(seatEntry);
						int seat = Integer.parseInt(st.nextToken());
						String name = st.nextToken();
						seatTable_.seats[seat] = name;
					}
				}
				synchronized (myClock) {
					myClock[myID] = Math.max(myClock[myID], theirClock) + 1;
					myClock[theirID] = Math.max(myClock[theirID], theirClock);
				}
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

					System.out.println("ClientRequest: " + rmi + " " + name);

					int index = -3; // initialize to unused val
					if (rmi.equals("reserve")) {
						getMutex();
						index = seatTable_.reserve(name);
						if (index < 0)
							releaseMutex("null", "null"); // **
						else
							releaseMutex("res", name);
					} else if (rmi.equals("search")) {
						index = seatTable_.search(name);
					} else if (rmi.equals("delete")) {
						getMutex();
						index = seatTable_.delete(name);
						if (index < 0)
							releaseMutex("null", "null"); // **
						else
							releaseMutex("del", name); // **
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
