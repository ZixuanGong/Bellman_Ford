import java.io.Serializable;
import java.net.InetAddress;

public class Client implements Serializable{
	private InetAddress ip;
	private int port;
	
	public Client(InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public String produceKey() {
		return ip.toString() + port;
	}
	
	public static boolean compare(Client c1, Client c2) {
		if (c1.produceKey().equals(c2.produceKey())) {
			return true;
		} else {
			return false;
		}
	}

	public InetAddress getIp() {return ip;}
	public int getPort() {return port;}
}