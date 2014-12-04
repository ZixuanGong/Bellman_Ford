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
	
	public void updateRt(HashMap<String, DistanceVector> dvMap_rcvd, Client sender) {
		boolean updated = false;

		float client_to_mp = sender.getWeight();
		//for each dest in rcvd rt
		for(DistanceVector s_dv: dvMap_rcvd.values()){
			float mp_to_mpdest = s_dv.getCost();
			Client dest = s_dv.getDest();

			if (neighbours.containsKey(dest.toKey()))
				if (neighbours.get(dest.toKey()).isDead())
					continue;

			if (s_dv.getLink() != null) {
				Client link = s_dv.getLink();
				if (neighbours.containsKey(link.toKey()))
					if (neighbours.get(link.toKey()).isDead())
						continue;

				if (Client.compare(s_dv.getLink(), currClient))
					continue;
			}

			if (Client.compare(dest, currClient)) {
				//dest is curr node
				DistanceVector new_link_dv = dvMap_rcvd.get(currClient.toKey());
				DistanceVector old_link_dv = dvMap.get(sender.toKey());
				float old_cost = old_link_dv.getCost();
				float new_cost = new_link_dv.getCost();
				//link restored
				if (old_cost > new_cost && Client.compare(new_link_dv.getLink(), currClient)) {
					old_link_dv.setCost(new_cost);
					old_link_dv.setLink(sender);
					updated = true;
				}

			}
			//compare with my dv -- if an entry exists
			else if (dvMap.containsKey(dest.toKey())) {
				DistanceVector m_dv = dvMap.get(dest.toKey());
				float client_to_dest = m_dv.getCost();
				float tempdist = client_to_mp + mp_to_mpdest;

				if (client_to_dest > tempdist) {

					m_dv.setLink(sender);
					m_dv.setCost(tempdist);
					updated = true;

				} else if (m_dv.getCost() == INFINITE) {

				} else if (Client.compare(m_dv.getLink(), sender)) {
					if (tempdist > INFINITE) {
						

						//if dest is neighbour
						if (neighbours.containsKey(dest.toKey())) {
							Client neighbour = neighbours.get(dest.toKey());
							m_dv.setLink(neighbour);
							m_dv.setCost(neighbour.getWeight());
						} else {
							m_dv.setLink(null);
							m_dv.setCost(INFINITE);
						}
					} else {
						m_dv.setCost(tempdist);
					}
					updated = true;
					
				}

			//new entry
			} else {
				float client_to_dest = client_to_mp + mp_to_mpdest;
				Client new_client = new Client(dest.getIp(), dest.getPort());
				DistanceVector new_dv = new DistanceVector(new_client, sender, client_to_dest);
				addToDvMap(new_dv);
				updated = true;
			}
		}

		if (updated) {
			sendRouteUpdate();
		}
	}

	public void handleLinkDown(Client c) {
		c.setLinkOn(false);
		
		//update own rt
		DistanceVector me2c = dvMap.get(c.toKey());
		
		//old dv is to go there directly
		if (Client.compare(me2c.getLink(), me2c.getDest())) {

			//update affected dv
			for(DistanceVector dv: dvMap.values()){
				Client dest = dv.getDest();
				//if c was along the path to dest
				if (dv.getLink() == null)
					continue;

				if (Client.compare(dv.getLink(), c)) {

					dv.setCost(dest.getWeight());
					if (dest.getWeight() < INFINITE)
						dv.setLink(dest);

					sendRouteUpdate();
				}
			}
		}
		
	}
	
	public void handleLinkUp(Client c) {
		c.setLinkOn(true);
		
		DistanceVector me2c = dvMap.get(c.toKey());
		if (c.getWeight() < me2c.getCost()) {

			me2c.setCost(c.getWeight());
			me2c.setLink(c);
			
			sendRouteUpdate();
		}	
	}

}