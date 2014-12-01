import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class RoutingTable {
	HashMap<String, DistanceVector> dvMap;
	HashMap<String, Client> neighbours;
	Client currClient;
	DatagramSocket sock;
	private static final float INFINITE = Float.MAX_VALUE;
	
	public static void dbg(String s) {
		System.out.println(s);
	}

	public RoutingTable(Client c, DatagramSocket sock) {
		dvMap = new HashMap<String, DistanceVector>();
		neighbours = new HashMap<String, Client>();
		currClient = c;
		this.sock = sock;
	}
	
	public void addToDvMap(DistanceVector dv) {
		dvMap.put(dv.getDest().produceKey(), dv);
	}
	
	public void addToNeighbours(Client c) {
		neighbours.put(c.produceKey(), c);
	}
	
	public void printRoutingTable() {
		String s;
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		s = format.format(new Date()) + " Distance vector list is:\n";
		
		for (DistanceVector dv: dvMap.values()) {
			s += "Distination=" + dv.getDest().getIp() + ":" + dv.getDest().getPort() + 
					", Cost=" + dv.getCost() + 
					", Link=" + dv.getLink().getIp() + ":" + dv.getLink().getPort() + "\n";
		}
		
		System.out.print(s);
	}
	
	public void sendRouteUpdate() {
		for (Client neighbour: neighbours.values()) {
			Packet pkt = new Packet(neighbour, dvMap);
			pkt.packPkt();
			byte[] pkt_bytes = pkt.getPkt_bytes();			
			DatagramPacket udp_pkt = new DatagramPacket(pkt_bytes, Array.getLength(pkt_bytes),
												neighbour.getIp(), neighbour.getPort());
			
			try {
				sock.send(udp_pkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	private void sendLinkDown(Client c) {
		Packet pkt = new Packet(c, dvMap);
		pkt.packPkt();
		byte[] pkt_bytes = pkt.getPkt_bytes();		
		DatagramPacket udp_pkt = new DatagramPacket(pkt_bytes, Array.getLength(pkt_bytes),
											c.getIp(), c.getPort());
		try {
			sock.send(udp_pkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateDv(DistanceVector dv) {
		
	}
	
	public void updateRt(HashMap<String, DistanceVector> dvMap_rcvd, Client sender) {
		//update dv with the sender first
		DistanceVector old_link_dv = dvMap.get(sender.produceKey());
		DistanceVector new_link_dv = dvMap_rcvd.get(currClient.produceKey());
		
		dbg("update rt");
		
		if (new_link_dv.getCost() != INFINITE)
			dbg("!=infinite");
		
		if (!Client.compare(old_link_dv.getLink(), sender))
			dbg("compare");
		
		if (new_link_dv.getCost() == INFINITE && 
				Client.compare(old_link_dv.getLink(), sender)) {
			
			dvMap.put(sender.produceKey(), new DistanceVector(sender, sender, INFINITE));
			neighbours.remove(sender.produceKey());
			dbg("link down");
			return;
		} else if (new_link_dv.getCost() < old_link_dv.getCost()) {
			new_link_dv.setDest(sender);
			if (new_link_dv.getLink().equals(currClient)) {
				new_link_dv.setLink(sender);
			} 
			
			dvMap.put(sender.produceKey(), new_link_dv);
		}
		
		for (DistanceVector dv: dvMap_rcvd.values()) {
			float new_cost;
			Client dest = dv.getDest();
			
			if (Client.compare(dest, currClient)) {
				continue;
			}
			new_cost = dvMap.get(sender.produceKey()).getCost() + dv.getCost();
			dv.setCost(new_cost);
			dv.setLink(sender);
			
			if (!dvMap.containsKey(dest)) {
				addToDvMap(dv);
			} else {
				DistanceVector curr_dv = dvMap.get(dest);
				if (curr_dv.getCost() > new_cost) {
					addToDvMap(dv);
				}
			}
		}
	}

	public void handleLinkDown(Client c) {
		
		neighbours.remove(c.produceKey());
		
		DistanceVector new_link_dv = new DistanceVector(c, c, INFINITE);
		DistanceVector old_link_dv = dvMap.get(c.produceKey());
		
		if (new_link_dv.getCost() == INFINITE && 
				Client.compare(old_link_dv.getLink(), c)) {
			
			dvMap.put(c.produceKey(), new_link_dv);
			sendRouteUpdate();
		}
		
		sendLinkDown(c);

	}

}