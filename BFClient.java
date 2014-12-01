import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class BFClient {
	RoutingTable rt;
	Client currClient;
	int timeout;
	private static final int MAX_DATAGRAM_LEN = 65507;
	private static final float INFINITE = Float.MAX_VALUE;
	
	public BFClient(String[] args) {
		int argc = Array.getLength(args);
		if (argc % 3 != 2 || argc < 5) {
			dbg("Error: Invalid number of argument");
			System.exit(-1);
		}

		try {
			int listen_port = Integer.parseInt(args[0]);
			timeout = Integer.parseInt(args[1]);
			
			DatagramSocket sock = new DatagramSocket(listen_port);
			currClient = new Client(InetAddress.getByName("127.0.0.1"), listen_port);
			rt = new RoutingTable(currClient, sock);

			for (int i = 0; i < argc/3; i++) {
				String ip = args[2 + i*3];
				int port = Integer.parseInt(args[3 + i*3]);
				float weight = Float.parseFloat(args[4 + i*3]);
				Client neighbour = new Client(InetAddress.getByName(ip), port);
				
				rt.addToDvMap(new DistanceVector(neighbour, neighbour, weight));
				rt.addToNeighbours(neighbour);
			}
			
			new ListenThread(sock).start();
			
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String input = stdIn.readLine();
				StringTokenizer st = new StringTokenizer(input);
				if (!st.hasMoreTokens()) {continue;}
				String token = st.nextToken();

				if (token.equals("LINKDOWN")) {
					
					if (!st.hasMoreTokens()) { dbg("Invalid command"); continue; }
					InetAddress ip = InetAddress.getByName(st.nextToken());
					if (!st.hasMoreTokens()) { dbg("Invalid command"); continue; }
					int port = Integer.parseInt(st.nextToken());
					linkdown(new Client(ip, port));
					
				} else if (token.equals("LINKUP")) {
					
					if (!st.hasMoreTokens()) { dbg("Invalid command"); continue; }
					InetAddress ip = InetAddress.getByName(st.nextToken());
					if (!st.hasMoreTokens()) { dbg("Invalid command"); continue; }
					int port = Integer.parseInt(st.nextToken());
					//TODO linkup()
					
				} else if (token.equals("SHOWRT")) {
					
					if (st.hasMoreTokens()) { dbg("Invalid command"); continue; }
					rt.printRoutingTable();
					
				} else if (token.equals("CLOSE")) {
					
					if (st.hasMoreTokens()) { dbg("Invalid command"); continue; }
					//TODO close()
				} else if (token.equals("SEND")) {
					synchronized (rt) {
						rt.sendRouteUpdate();
					}
				}
			}
		} catch(NumberFormatException e) {
			dbg("Error: Invalid Arguments");
			System.exit(-1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void linkdown(Client client) {
		synchronized (rt) {
			rt.handleLinkDown(client);
		}
	}

	public static void main(String[] args) {
		new BFClient(args);
		
	}

	public static void dbg(String s) {
		System.out.println(s);
	}
	
	private class ListenThread extends Thread {
		DatagramSocket sock;
		
		public ListenThread(DatagramSocket sock) {
			this.sock = sock;
		}

		@Override
		public void run() {
			byte[] tmp = new byte[MAX_DATAGRAM_LEN];
			DatagramPacket udp_pkt = new DatagramPacket(tmp, Array.getLength(tmp)); 
			while (true) {
				try {
					sock.receive(udp_pkt);
					dbg("receive msg");
					Packet pkt = new Packet();
					pkt.setPkt_bytes(udp_pkt.getData());
					pkt.unpackPkt();
					HashMap<String, DistanceVector> dvMap_rcvd = pkt.getDvMap();
					Client sender = new Client(udp_pkt.getAddress(), udp_pkt.getPort());
					
					synchronized (rt) {
						rt.updateRt(dvMap_rcvd, sender);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}
