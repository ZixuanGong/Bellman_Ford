import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.Timer;

public class BFClient implements ActionListener{
	RoutingTable rt;
	Client currClient;
	int timeout;
	Timer timer;
	private long now;
	private static final int MAX_DATAGRAM_LEN = 65507;
	private static final float INFINITE = 99999;
	
	public BFClient(String[] args) {
		int argc = Array.getLength(args);
		if (argc % 3 != 2 || argc < 5) {
			dbg("Error: Invalid number of argument");
			System.exit(-1);
		}

		try {
			int listen_port = Integer.parseInt(args[0]);
			timeout = 1000 * Integer.parseInt(args[1]);
			timer = new Timer (timeout, this);
			timer.start ();
			
			DatagramSocket sock = new DatagramSocket(listen_port);
			currClient = new Client(InetAddress.getByName("127.0.0.1"), listen_port);
			rt = new RoutingTable(currClient, sock);

			for (int i = 0; i < argc/3; i++) {
				String ip = args[2 + i*3];
				int port = Integer.parseInt(args[3 + i*3]);
				float weight = Float.parseFloat(args[4 + i*3]);
				Client neighbour = new Client(InetAddress.getByName(ip), port);
				neighbour.setLinkOn(true);
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
					sock.close();
					timer.stop();
					System.exit(0);
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

	private void linkup(Client c) {
		synchronized (rt) {
			if (!rt.neighbours.containsKey(c.toKey())) {
				dbg("Invalid link: not a neighbour");
			} else if (rt.neighbours.get(c.toKey()).isLinkOn()) {
				dbg("Invalid link: link is already on");
			} else {
				rt.handleLinkUp(c);
				rt.sendLinkMsg(c, Packet.LINKUP);
			}
		}
	}

	private void linkdown(Client c) {
		synchronized (rt) {
			if (!rt.neighbours.containsKey(c.toKey())) {
				dbg("Invalid link: not a neighbour");
			} else if (!rt.neighbours.get(c.toKey()).isLinkOn()) {
				dbg("Invalid link: link already destroyed");
			} else {
				rt.handleLinkDown(c);
				rt.sendLinkMsg(c, Packet.LINKDOWN);
			}
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
					Client c = new Client(udp_pkt.getAddress(), udp_pkt.getPort());
					Client sender = rt.neighbours.get(c.toKey());
					sender.setLinkOn(true);
					sender.setTimestamp(System.currentTimeMillis());
					
					Packet pkt = Packet.unpackPkt(udp_pkt.getData());					
					
					synchronized (rt) {
						switch (pkt.getMsg_type()) {
						case Packet.UPDATE:
							dbg("receive UPDATE msg from " + sender.toKey());
							HashMap<String, DistanceVector> dvMap_rcvd = pkt.getDvMap();
							rt.updateRt(dvMap_rcvd, sender);
							break;
							
						case Packet.LINKDOWN:
							dbg("receive LINKDOWN msg from " + sender.toKey());
							rt.handleLinkDown(sender);
							break;
							
						case Packet.LINKUP:
							dbg("receive LINKUP msg from " + sender.toKey());
							rt.handleLinkUp(sender);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		try{
			rt.sendRouteUpdate();
			timer.restart();

			for(Client c: rt.neighbours.values()){
				if (!c.isLinkOn())
					continue;

				if ((c.getTimestamp() + 3 * timeout < System.currentTimeMillis())){
					rt.handleLinkDown(c);
				}
			}

		}catch(Exception exception){
			exception.printStackTrace();
		}
	}
	
}
