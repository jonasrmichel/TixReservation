import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class TicketClient {
	static Random randy = new Random();

	static Socket getRandomServerPort(List<Integer> portList) {
		while (true) {
			try {
				int tryPort = portList.get(randy.nextInt(Symbols.maxServers));
				System.out.print("Trying port " + tryPort + "...");
				Socket server = new Socket(Symbols.ticketServer, tryPort);
				System.out.println("SUCCESS");
				return server;
			} catch (IOException e) {
				System.out.println("NOT LISTENING");
			}
		}
	}

	public static void main(String[] args) {
		BufferedReader buf = new BufferedReader(
				new InputStreamReader(System.in));
		String line = "";
		String response;
		Symbols.initServerLists();

		while (true && line != null) {
			System.out.print("Command: ");
			try {
				line = buf.readLine();
			} catch (IOException e) {
				// don't care
			}
			boolean succeed = false;
			while (!succeed && line != null) {
				try {
					Socket server = getRandomServerPort(Symbols.serverList_Public);
					server.setSoTimeout(Symbols.timeout);
					BufferedReader din = new BufferedReader(
							new InputStreamReader(server.getInputStream()));
					PrintStream pout = new PrintStream(server.getOutputStream());

					pout.println(line);
					pout.flush();
					response = din.readLine();
					if (Integer.parseInt(response) == Symbols.invalid)
						System.out.println("Unrecognized Command:" + line);
					else
						System.out.println("Result: " + response);
					succeed = true;
				} catch (Exception e) {
					System.err.println("Client aborted: " + e);
				}
			}
		}
	}
}
