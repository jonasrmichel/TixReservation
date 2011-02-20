import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientHandlerRunner implements Runnable {

	protected Socket clientSocket_ = null;
	protected SeatTable seatTable_ = null;

	public ClientHandlerRunner(Socket clientSocket, SeatTable seatTable) {
		this.clientSocket_ = clientSocket;
		this.seatTable_ = seatTable;
	}

	@Override
	public void run() {

		try {
			BufferedReader din = new BufferedReader(new InputStreamReader(
					clientSocket_.getInputStream()));
			PrintWriter pout = new PrintWriter(clientSocket_.getOutputStream());
			String getline = din.readLine();
			StringTokenizer st = new StringTokenizer(getline);

			String rmi = st.nextToken();
			String name = st.nextToken();

			System.out.println("Request: " + rmi + " " + name);

			int index = -3; // initialize to unused val
			// TODO: mutex
			if (rmi.equals("reserve")) {
				index = seatTable_.reserve(name);
			} else if (rmi.equals("search")) {
				index = seatTable_.search(name);
			} else if (rmi.equals("delete")) {
				index = seatTable_.delete(name);
			}
			pout.println(index);
			pout.flush();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

}
