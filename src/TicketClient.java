import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class TicketClient {
	BufferedReader din;
	PrintStream pout;

	public void getSocket() throws IOException {
		Socket server = new Socket(Symbols.ticketServer, Symbols.serverPort);
		din = new BufferedReader(
				new InputStreamReader(server.getInputStream()));
		pout = new PrintStream(server.getOutputStream());
	}

	static boolean sendToServer(PrintStream ps, String tag, String rmi, String param) {
		ps.println(tag + " " + rmi + " " + param);
		ps.flush();
		return true;
	}

	public int reserveName(String name) throws IOException {
		getSocket();
		sendToServer(pout, Symbols.clientTag, "reserve", name);
		return Integer.parseInt(din.readLine());
	}

	public int searchName(String name) throws IOException {
		getSocket();
		sendToServer(pout, Symbols.clientTag, "search", name);
		return Integer.parseInt(din.readLine());
	}

	public int deleteName(String name) throws IOException {
		getSocket();
		sendToServer(pout, Symbols.clientTag, "delete", name);
		return Integer.parseInt(din.readLine());
	}

	public static void main(String[] args) {
		TicketClient myClient = new TicketClient();
		try {
			String aName = "Maximus";
			System.out.println("reserve returned: " + myClient.reserveName(aName));
			System.out.println("search returned: " + myClient.searchName(aName));
			System.out.println("delete returned: " + myClient.deleteName(aName));
		} catch (Exception e) {
			System.err.println("Client aborted: " + e);
		}
	}
}
