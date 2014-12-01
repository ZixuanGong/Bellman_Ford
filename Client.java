import java.io.Serializable;
import java.net.InetAddress;

public class Client implements Serializable{
	private InetAddress ip;
	private int port;
	private float weight;
	private boolean isNeighbour = false;
	private boolean linkOn = false;
	
	public Client(InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public String toKey() {
		return ip.toString() + port;
	}
	
	public static boolean compare(Client c1, Client c2) {
		if (c1.toKey().equals(c2.toKey())) {
			return true;
		} else {
			return false;
		}
	}

	public InetAddress getIp() {return ip;}
	public int getPort() {return port;}

	public boolean isNeighbour() {
		return isNeighbour;
	}

	public void setNeighbour(boolean isNeighbour) {
		this.isNeighbour = isNeighbour;
	}

	public boolean isLinkOn() {
		return linkOn;
	}

	public void setLinkOn(boolean linkOn) {
		this.linkOn = linkOn;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	
	
}