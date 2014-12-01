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
				neighbour.setLinkOn(true);
				neighbour.setNeighbour(true);
				neighbour.setWeight(weight);
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
					linkup(new Client(ip, port));
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

	private void linkup(Client client) {
		synchronized (rt) {
			boolean ret = rt.handleLinkUp(client);
			if(!ret) {
				dbg("Invalid link");
			}
		}
	}

	private void linkdown(Client c) {
		synchronized (rt) {
			if (!rt.neighbours.containsKey(c.toKey()) ||
					rt.neighbours.get(c.toKey()).isLinkOn()) {
				dbg("Invalid link");
			}
			
			rt.handleLinkDown(c);
			rt.sendLinkDown(c);
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
					Client sender = new Client(udp_pkt.getAddress(), udp_pkt.getPort());
					dbg("receive msg");
					
//					byte[] pkt_bytes = new byte[udp_pkt.getLength()];
//					System.arraycopy(udp_pkt.getData(), 0, pkt_bytes, 0, udp_pkt.getLength());
					
					Packet pkt = Packet.unpackPkt(udp_pkt.getData());					
					
					synchronized (rt) {
						switch (pkt.getMsg_type()) {
						case Packet.UPDATE:
							HashMap<String, DistanceVector> dvMap_rcvd = pkt.getDvMap();
							rt.updateRt(dvMap_rcvd, sender);
							break;
							
						case Packet.LINKDOWN:
							rt.handleLinkDown(sender);
							break;
							
						case Packet.LINKUP:
							
							break;

						default:
							break;
						}
					}
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}
