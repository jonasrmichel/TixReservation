import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientHandlerRunner implements Runnable {

	protected ServerSocket serverSocket_ = null;
	protected SeatTable seatTable_ = null;

	public ClientHandlerRunner(ServerSocket serverSocket, SeatTable seatTable) {
		this.serverSocket_ = serverSocket;
		this.seatTable_ = seatTable;
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
