import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class TicketClient {
	BufferedReader din;
	PrintStream pout;

	public void getSocket() throws IOException {
		Socket server = new Socket(Symbols.ticketServer, Symbols.serverPort);
		din = new BufferedReader(
				new InputStreamReader(server.getInputStream()));
		pout = new PrintStream(server.getOutputStream());
	}

	public int reserveName(String name) throws IOException {
		getSocket();
		pout.println("reserve " + name);
		pout.flush();
		return Integer.parseInt(din.readLine());
	}

	public int searchName(String name) throws IOException {
		getSocket();
		pout.println("search " + name);
		pout.flush();
		return Integer.parseInt(din.readLine());
	}

	public int deleteName(String name) throws IOException {
		getSocket();
		pout.println("delete " + name);
		pout.flush();
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
