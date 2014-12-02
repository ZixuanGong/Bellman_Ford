import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;


public class Packet implements Serializable {
	public static final int LINKDOWN = 1;
	public static final int LINKUP = 2;
	public static final int UPDATE = 3;
	
	private Client destClient;
	private HashMap<String, DistanceVector> dvMap;
	private int msg_type;
	
	public Packet(Client destClient, HashMap<String, DistanceVector> dvMap, int type) {
		this.destClient = destClient;
		this.dvMap = dvMap;
		msg_type = type;
	}
	
	public Packet() {
		
	}
	
	@SuppressWarnings("finally")
	public static byte[] packPkt(Packet pkt) {
		byte[] pkt_bytes = null;
		try {
			pkt_bytes = serialize(pkt);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return pkt_bytes;
		}
	}
	
	@SuppressWarnings("finally")
	public static Packet unpackPkt(byte[] pkt_bytes) {
		Packet pkt = null;
		try {
			pkt = (Packet) deserialize(pkt_bytes);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			return pkt;
		}
	}
	
	public HashMap<String, DistanceVector> getDvMap() {
		return dvMap;
	}

	public Client getDestClient() {
		return destClient;
	}

	public void setDestClient(Client destClient) {
		this.destClient = destClient;
	}

	public int getMsg_type() {
		return msg_type;
	}

	public void setMsg_type(int msg_type) {
		this.msg_type = msg_type;
	}

	public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }
	
	public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }
	
}
