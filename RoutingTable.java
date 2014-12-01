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
		dvMap.put(dv.getDest().toKey(), dv);
	}
	
	public void addToNeighbours(Client c) {
		neighbours.put(c.toKey(), c);
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
			dbg(neighbour.toKey());
			
			if (!neighbour.isLinkOn())
				continue;
			
			Packet pkt = new Packet(neighbour, dvMap, Packet.UPDATE);
//			pkt.packPkt();
			byte[] pkt_bytes = Packet.packPkt(pkt);		
			DatagramPacket udp_pkt = new DatagramPacket(pkt_bytes, Array.getLength(pkt_bytes),
							neighbour.getIp(), neighbour.getPort());
			
			dbg("send to " + neighbour.toKey());

			try {
				sock.send(udp_pkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public void sendLinkDown(Client c) {
		Packet pkt = new Packet(c, null, Packet.LINKDOWN);
//		pkt.packPkt();
		byte[] pkt_bytes = Packet.packPkt(pkt);		
		DatagramPacket udp_pkt = new DatagramPacket(pkt_bytes, Array.getLength(pkt_bytes),
											c.getIp(), c.getPort());
		try {
			sock.send(udp_pkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateDv(DistanceVector new_link_dv) {
		Client dest = new_link_dv.getDest();
		DistanceVector old_link_dv = dvMap.get(dest.toKey());
		if (new_link_dv.getCost() == INFINITE && 
				Client.compare(old_link_dv.getLink(), dest)) {
			
			dvMap.put(dest.toKey(), new DistanceVector(dest, dest, INFINITE));
			sendRouteUpdate();
			dbg("updateDv");
		}
	}
	
	public void updateRt(HashMap<String, DistanceVector> dvMap_rcvd, Client sender) {
		//update dv with the sender first
		DistanceVector old_link_dv = dvMap.get(sender.toKey());
		DistanceVector new_link_dv = dvMap_rcvd.get(currClient.toKey());
		
		dbg("update rt");
		
		if (new_link_dv.getCost() < old_link_dv.getCost()) {
			new_link_dv.setDest(sender);
			if (new_link_dv.getLink().equals(currClient)) {
				new_link_dv.setLink(sender);
			} 
			
			dvMap.put(sender.toKey(), new_link_dv);
		}
		
		for (DistanceVector dv: dvMap_rcvd.values()) {
			float new_cost;
			Client dest = dv.getDest();
			
			if (Client.compare(dest, currClient)) {
				continue;
			}
			new_cost = dvMap.get(sender.toKey()).getCost() + dv.getCost();
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
		
		neighbours.get(c.toKey()).setLinkOn(false);
		
		//update own rt
		DistanceVector new_link_dv = new DistanceVector(c, c, INFINITE);
		updateDv(new_link_dv);
	}
	
	public boolean handleLinkUp(Client c) {
		if (!neighbours.containsKey(c.toKey()) ||
				neighbours.get(c.toKey()).isLinkOn()) {
			return false;
		}
		
		neighbours.get(c.toKey()).setLinkOn(true);
		sendLinkDown(c);
		
		DistanceVector new_link_dv = new DistanceVector(c, c, c.getWeight());
		DistanceVector old_link_dv = dvMap.get(c.toKey());
		
		if (new_link_dv.getCost() < old_link_dv.getCost()) {
		
			dvMap.put(c.toKey(), new_link_dv);
			sendRouteUpdate();
		}		
		return true;
	}

}