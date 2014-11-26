import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;


public class BFClient {
	ArrayList<ClientInfo> client_list = new ArrayList<ClientInfo>();
	int listen_port;
	int timeout;
	
	public BFClient(String[] args) {
		int argc = Array.getLength(args);
		if (argc % 3 != 2 || argc < 5) {
			dbg("Error: Invalid number of argument");
			System.exit(-1);
		}

		try {
			listen_port = Integer.parseInt(args[0]);
			timeout = Integer.parseInt(args[1]);

			for (int i = 0; i < argc/3; i++) {
				String neighbour_ip = args[2 + i*3];
				int neighbour_port = Integer.parseInt(args[3 + i*3]);
				float neighbour_weight = Float.parseFloat(args[4 + i*3]);
				
				client_list.add(new ClientInfo(neighbour_ip, neighbour_port, neighbour_weight));
			}
			
			new ListenThread().start();
			
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String input;
				if ((input = stdIn.readLine()) != null) {
					out.println(input);
				}
			}
			
		} catch(NumberFormatException e) {
			dbg("Error: Invalid Arguments");
			System.exit(-1);
		}
		
	}

	public static void main(String[] args) {
		new BFClient(args);
		
	}

	public static void dbg(String s) {
		System.out.println(s);
	}
	
	private class ListenThread extends Thread {

		@Override
		public void run() {
			try {
				DatagramSocket listen_sock = new DatagramSocket(listen_port);
			} catch (SocketException e) {
				e.printStackTrace();
			} 
		}
		
	}
}
