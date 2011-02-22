import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class AutoClientRunner extends TicketClient implements Runnable {
	public static void main(String[] args) {
		Symbols.initServerLists();

		for (int i = 10; i > 0; --i) {
			new Thread(new AutoClientRunner(i)).start();
		}

	}

	int ident;

	public AutoClientRunner(int i) {
		ident = i;
	}

	Object lock = new Object();

	String[] cmds = { "reserve yourmom", "search yourmom", "delete yourmom",
			"reserve ALICE", "search ALICE", "delete ALICE", "reserve bob",
			"search bob", "delete bob", "reserve charlie", "search charlie",
			"delete charlie" };

	@Override
	public void run() {
		for (int i = 0; i < 5; ++i) {
			try {
				System.out.print(ident);
				Socket clientSocket = getRandomServerPort(Symbols.serverList_Public);
				clientSocket.setSoTimeout(Symbols.timeout);
				BufferedReader din = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				PrintWriter pout = new PrintWriter(
						clientSocket.getOutputStream());
				String next = cmds[randy.nextInt(cmds.length)];
				synchronized (lock) {
					System.out.println(ident + " trying " + next);
					pout.println(next);
					pout.flush();
				}

				System.out.println("Res to " + ident + " for " + next + " "
						+ din.readLine());

			} catch (SocketTimeoutException ex) {
				// Blarg
				ex.printStackTrace();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}