import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

public class TicketServer {
	SeatTable seatTable_;
	int myID;
	volatile long[] myClock = new long[Symbols.maxServers];
	Socket[] connections = new Socket[Symbols.maxServers];
	volatile PriorityQueue<Request> requests = new PriorityQueue<Request>(
			Symbols.maxServers);

	/**
	 * ctor
	 */
	public TicketServer() {
		seatTable_ = new SeatTable();
		for (int i = 0; i < Symbols.maxServers; ++i) {
			myClock[i] = 0;
		}
		Symbols.initServerLists();
	}

	/**
	 * Iterates through a list of ports until a successful ServerSocket is setup
	 * at a port.
	 *
	 * @param portList
	 * @return ServerSocket
	 * @throws MaxServersReachedException
	 */
	static ServerSocket getUnusedPort(List<Integer> portList)
			throws MaxServersReachedException {
		// Try ports in portList until successful
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

	/**
	 * Creates a single ServerSocket listener for client requests (public),
	 * establishes a connection to all other active servers (private), synchs
	 * its local copy of seat table, and begins listening for client requests /
	 * server messages.
	 *
	 * @throws MaxServersReachedException
	 */
	public void serverRunLoop() throws MaxServersReachedException {
		ServerSocket listener = getUnusedPort(Symbols.serverList_Public);
		ServerSocket serverListener = getUnusedPort(Symbols.serverList_Private);
		try {
			serverListener.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		myID = serverListener.getLocalPort() - Symbols.basePort_Private;
		myClock[myID] = 1;
		int firstServerWeFind = -1;
		// identify other active servers
		for (int i : Symbols.serverList_Private) {
			if (i == serverListener.getLocalPort())
				continue;
			try {
				Socket server = new Socket(Symbols.ticketServer, i);
				server.setSoTimeout(Symbols.timeout);
				PrintWriter pout = new PrintWriter(server.getOutputStream());
				// let them know we're a new server
				pout.println("hey " + myID + " " + 1);
				pout.flush();
				// get their response
				BufferedReader br = new BufferedReader(new InputStreamReader(
						server.getInputStream()));
				String line = br.readLine();
				System.out.println(line);
				// start listening on this connection for server-server messages
				new Thread(new ServerHandlerRunner(server)).start();
				// they should have responded with an "ack"
				StringTokenizer st = new StringTokenizer(line);
				String rmi = st.nextToken();
				assert rmi.equals("ack");
				int theirID = Integer.parseInt(st.nextToken());
				// keep track of server ID and socket pairing (who's who)
				connections[theirID] = server;
				if (firstServerWeFind == -1)
					firstServerWeFind = theirID;
				// Lamport: update my clock
				long theirClock = Long.parseLong(st.nextToken()); // clock
				myClock[myID] = Math.max(myClock[myID], theirClock) + 1;
				myClock[theirID] = Math.max(myClock[theirID], theirClock);
			} catch (IOException ex) {
				// Server is not up...ignore.
			}
		}
		// synchronize local copy of seat table with the first server we saw
		if (firstServerWeFind != -1) {
			try {
				Socket firstguy = connections[firstServerWeFind];
				PrintWriter pout = new PrintWriter(firstguy.getOutputStream());
				// make sure no one is modifying while we synchronize
				getMutex();
				// request the seat table
				pout.println("gst " + myID + " " + myClock[myID]);
				pout.flush();
			} catch (IOException ex) {
				// This shouldn't happen?
				assert false;
			}
		}
		// start listening for client requests
		new Thread(new ClientHandlerRunner(listener)).start();
		// listen for server-server messages
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
			tixServer.serverRunLoop();
		} catch (MaxServersReachedException e) {
			System.err.println("Server aborted: " + e);
		}
	}

	/**
	 * Lamport: Mutex Release. Notifies all other servers of critical section
	 * release and of the seat table modification made. Removes the request from
	 * the requests queue.
	 *
	 * @param command
	 * @param name
	 */
	private void releaseMutex(String command, String name) {
		System.out.println("releasing mutex!");
		// send a release to all other servers
		for (int i = 0; i < connections.length; ++i) {
			Socket s = connections[i];
			if (s == null) {
				continue;
			}
			try {
				PrintWriter pout = new PrintWriter(s.getOutputStream());
				pout.println("rel " + myID + " " + myClock[myID] + " "
						+ command + " " + name);
				pout.flush();
			} catch (IOException e) {
				// Server is dead.
				connections[i] = null;
			}
		}
		++myClock[myID];
		// remove myself from the requests queue
		synchronized (requests) {
			requests.remove();
			requests.notifyAll();
		}
		System.out.println("released mutex!");
	}

	/**
	 * Lamport: Request Mutex. Sends a request for the critical section to all
	 * other servers. Inserts a request into the request queue. Waits until
	 * access to the critical section is obtained.
	 */
	private void getMutex() {
		System.out.println("getting mutex!");
		synchronized (myClock) {
			// send request to all other servers
			for (int i = 0; i < connections.length; ++i) {
				Socket s = connections[i];
				if (s == null) {
					continue;
				}
				try {
					PrintWriter pout = new PrintWriter(s.getOutputStream());
					System.out.println("sending:" + "req " + myID + " "
							+ myClock[myID]);
					pout.println("req " + myID + " " + myClock[myID]);
					pout.flush();
				} catch (IOException e) {
					// Server is dead.
					connections[i] = null;
				}
			}
			// add myself to the requests queue
			synchronized (requests) {
				System.out.println("adding myself to requests");
				requests.add(new Request(myID, myClock[myID]));
			}
			++myClock[myID];
		}

		// wait until I get access to the critical section
		synchronized (requests) {
			while (notMutex()) {
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

	/**
	 * Lamport: Critical Section Access Check. Check to determine if I may
	 * access the critical section.
	 *
	 * @return true (not ready), false (ready)
	 */
	private boolean notMutex() {
		if (requests.peek().id_ != myID)
			return true;
		for (int i = 0; i < myClock.length; ++i) {
			if (connections[i] != null && myClock[i] < requests.peek().clock_)
				return true;
		}
		return false;
	}

	/**
	 * Runnable class to handle incoming server-server messages.
	 *
	 */
	public class ServerHandlerRunner implements Runnable {

		protected Socket clientSocket = null;

		public ServerHandlerRunner(Socket serverSocket) {
			this.clientSocket = serverSocket;
		}

		BufferedReader din;
		PrintWriter pout;
		int theirID;
		boolean hasFailed = false;

		@Override
		public void run() {

			try {
				din = new BufferedReader(new InputStreamReader(clientSocket
						.getInputStream()));
				pout = new PrintWriter(clientSocket.getOutputStream());
			} catch (IOException e) {
				// shouldn't happen
				assert false;
			}

			// message handler loop
			while (true) {
				try {
					String getline = din.readLine();
					System.out.println(myClock[myID] + ": " + getline);
					StringTokenizer st = new StringTokenizer(getline);

					// server-server messages prefixed with: msg id clock
					String rmi = st.nextToken(); // msg
					int theirID = Integer.parseInt(st.nextToken()); // id
					long theirClock = Long.parseLong(st.nextToken()); // clock

					// Lamport: update my clock
					synchronized (myClock) {
						myClock[myID] = Math.max(myClock[myID], theirClock) + 1;
						myClock[theirID] = Math.max(myClock[theirID],
								theirClock);
					}

					// handle message types
					if (rmi.equals("req")) { // request
						// add the request to requests queue
						synchronized (requests) {
							requests.add(new Request(theirID, theirClock));
						}
						// acknowledge request
						pout.println("ack " + myID + " " + myClock[myID]);
						pout.flush();
					} else if (rmi.equals("rel")) { // release
						String mod = st.nextToken(); // type of modification
						// made
						String name = st.nextToken(); // modification data
						// remove the request from the requests queue
						synchronized (requests) {
							requests.remove();
							requests.notifyAll();
						}
						// update my local seat table accordingly
						if (mod.equals("res")) { // modification: reserve
							seatTable_.reserve(name);
						} else if (mod.equals("del")) { // modification: delete
							seatTable_.delete(name);
						}
					} else if (rmi.equals("hey")) { // new server
						pout.println("ack " + myID + " " + myClock[myID]);
						pout.flush();
						// keep track of id and socket pair
						this.theirID = theirID;
						connections[theirID] = clientSocket;
					} else if (rmi.equals("gst")) { // get seat table
						// notify that I'm ready to send my table
						pout.println("rdy " + myID + " " + myClock[myID] + " "
								+ seatTable_.getCount());
						pout.flush();
						// send my seat table contents
						int sent = 0;
						for (int i = 0; i < seatTable_.maxSize
								&& sent < seatTable_.getCount(); ++i) {
							if (seatTable_.emptySeat(seatTable_.seatedAt(i)))
								continue;
							pout.println(i + " " + seatTable_.seatedAt(i));
							pout.flush();
							++sent;
						}
					} else if (rmi.equals("rdy")) { // get ready to receive seat
						// table
						int numInTable = Integer.parseInt(st.nextToken());
						seatTable_.setCount(numInTable);
						for (int i = 0; i < numInTable; ++i) {
							String seatEntry = din.readLine();
							st = new StringTokenizer(seatEntry);
							int seat = Integer.parseInt(st.nextToken());
							String name = st.nextToken();
							seatTable_.insert(seat, name);
						}
						// release the mutex, nothing was changed
						releaseMutex("null", "null");
					} else if (rmi.equals("ack")) {
						synchronized (requests) {
							requests.notifyAll();
						}
					}
					// reset failure switch
					hasFailed = false;
				} catch (IOException e) {
					// Server died?
					if (hasFailed) {
						// server is dead, let this thread die
						connections[theirID] = null;
						return;
					}
					// server timeout, ping them, are they alive?
					hasFailed = true;
					pout.println("hey " + myID + " " + myClock[myID]);
					pout.flush();
				}
			}
		}
	}

	/**
	 * Runnable class to handle incoming client-server requests.
	 *
	 */
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
					PrintWriter pout = new PrintWriter(clientSocket
							.getOutputStream());
					String getline = din.readLine();
					StringTokenizer st = new StringTokenizer(getline);

					int index = Symbols.invalid;

					// error handling
					if (st.countTokens() != 2) {
						pout.println(index);
						pout.flush();
						continue;
					}

					String rmi = st.nextToken();
					String name = st.nextToken();

					System.out.println("ClientRequest: " + rmi + " " + name);

					if (rmi.equals("reserve")) {
						getMutex();
						index = seatTable_.reserve(name);
						if (index < 0)
							releaseMutex("null", "null");
						else
							releaseMutex("res", name);
					} else if (rmi.equals("search")) {
						// don't need mutex to read
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
