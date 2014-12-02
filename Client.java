import java.io.Serializable;
import java.net.InetAddress;

public class Client implements Serializable{
	private static final float INFINITE = 99999;
	private InetAddress ip;
	private int port;
	private float weight;
	private boolean linkOn = false;
	private long timestamp;
	private boolean dead;
	
	public Client(InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
		timestamp = System.currentTimeMillis();
	}
	
	public String toKey() {
		return ip.toString() + port;
	}
	
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
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

	public boolean isLinkOn() {
		return linkOn;
	}

	public void setLinkOn(boolean linkOn) {
		this.linkOn = linkOn;
	}

	public float getWeight() {
		if (isLinkOn())
			return weight;
		else
			return INFINITE;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	
	
}