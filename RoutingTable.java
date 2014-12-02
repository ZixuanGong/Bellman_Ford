import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class RoutingTable {
	HashMap<String, DistanceVector> dvMap;
	HashMap<String, Client> neighbours;
	Client currClient;
	DatagramSocket sock;
	private static final float INFINITE = 99999;
	
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
					", Cost=" + dv.getCost();

			if (dv.getCost() != INFINITE)
				s += ", Link=" + dv.getLink().getIp() + ":" + dv.getLink().getPort();

			s += "\n";
		}
		
		System.out.print(s);
	}
	
	public void sendRouteUpdate() {
		for (Client neighbour: neighbours.values()) {			
			if (!neighbour.isLinkOn())
				continue;
			
			Packet pkt = new Packet(neighbour, dvMap, Packet.UPDATE);
			byte[] pkt_bytes = Packet.packPkt(pkt);		
			DatagramPacket udp_pkt = new DatagramPacket(pkt_bytes, Array.getLength(pkt_bytes),
							neighbour.getIp(), neighbour.getPort());
			
			try {
				sock.send(udp_pkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}

	public void sendLinkMsg(Client c, int msg_type) {
		Packet pkt = new Packet(c, null, msg_type);
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
				Client.compare(old_link_dv.getLink(), old_link_dv.getDest())) {
			
			addToDvMap(new_link_dv);

			for(DistanceVector dv: dvMap.values()){ 
				if (Client.compare(dv.getLink(), dest)) 
					dv.setCost(INFINITE);
			}
			sendRouteUpdate();
			dbg("updateDv: linkdown");
		} else if (new_link_dv.getCost() < old_link_dv.getCost()) {
		
			addToDvMap(new_link_dv);
			sendRouteUpdate();
			dbg("updateDv: linkup");
		}	
	}
	
	public void updateRt(HashMap<String, DistanceVector> dvMap_rcvd, Client sender) {
		boolean updated = false;

		float client_to_mp = dvMap.get(sender.toKey()).getCost();
		//for each dest in rcvd rt
		for(DistanceVector s_dv: dvMap_rcvd.values()){
			float mp_to_mpdest = s_dv.getCost();
			Client dest = s_dv.getDest();

			if (Client.compare(dest, currClient)) {
				//dest is curr node
				DistanceVector new_link_dv = dvMap_rcvd.get(currClient.toKey());
				DistanceVector old_link_dv = dvMap.get(sender.toKey());
				//if Me-Midpoint > Midpoint-Me and neighbors, update Me-Midpoint
				float me_mp = old_link_dv.getCost();
				float mp_me = new_link_dv.getCost();
				//link restored?
				if (me_mp > mp_me && Client.compare(new_link_dv.getLink(), currClient)) {
					old_link_dv.setCost(new_link_dv.getCost());
					old_link_dv.setLink(sender);
					
					addToDvMap(old_link_dv);
					updated = true;
				}
			}
			//compare with my dv -- if an entry exists
			else if (dvMap.containsKey(dest.toKey())) {
				DistanceVector m_dv = dvMap.get(dest.toKey());
				float client_to_dest = m_dv.getCost();
				float tempdist = client_to_mp + mp_to_mpdest;

				if(client_to_dest > tempdist) {
					//client - midpoint - dest
					m_dv.setLink(sender);
					m_dv.setCost(tempdist);
					addToDvMap(m_dv);
					updated = true;
				}
				if (Client.compare(m_dv.getLink(), sender) && 
						!Client.compare(m_dv.getLink(), s_dv.getLink())) {
						if(tempdist > INFINITE)
							tempdist = INFINITE;

						//client - midpoint - dest
						m_dv.setLink(sender);
						m_dv.setCost(tempdist);
						addToDvMap(m_dv);
				}
			} else {
				float client_to_dest = client_to_mp + mp_to_mpdest;
				DistanceVector new_dv = new DistanceVector(dest, sender, client_to_dest);
				addToDvMap(new_dv);
			}
		}

		if (updated)
			sendRouteUpdate();
	}

	public void handleLinkDown(Client c) {
		neighbours.get(c.toKey()).setLinkOn(false);
		
		//update own rt
		DistanceVector new_link_dv = new DistanceVector(c, c, INFINITE);
		updateDv(new_link_dv);
	}
	
	public void handleLinkUp(Client c) {
		neighbours.get(c.toKey()).setLinkOn(true);
		
		DistanceVector new_link_dv = new DistanceVector(c, c, neighbours.get(c.toKey()).getWeight());
		updateDv(new_link_dv);
	}

}