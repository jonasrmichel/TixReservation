import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class TicketClient {
	static Socket getRandomServerPort(List<Integer> portList)
		throws MaxServersReachedException {
		Random randy = new Random();
		while (true) {
			try {
				int tryPort = portList.get(randy.nextInt(Symbols.maxServers));
				System.out.print("Trying port " + tryPort + "...");
				Socket server = new Socket(Symbols.ticketServer, tryPort);
				System.out.println("SUCCESS");
				return server;
			} catch (IOException e) {
				System.out.println("IN USE");
			}
		}
	}

	public static void main(String[] args) {
		BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		Symbols.initServerLists();

		while (true) {
			try {
				line = buf.readLine();
			} catch (IOException e) {
				// don't care
			}
			boolean succeed = false;
			while(!succeed) {
				try {
					Socket server = getRandomServerPort(Symbols.serverList_Public);
					server.setSoTimeout(Symbols.TIMEOUT_);
					BufferedReader din = new BufferedReader(
							new InputStreamReader(server.getInputStream()));
					PrintStream pout = new PrintStream(server.getOutputStream());

					pout.println(line);
					pout.flush();
					System.out.println(din.readLine());
					succeed = true;
				} catch (Exception e) {
					System.err.println("Client aborted: " + e);
				}
			}
		}
	}
}
