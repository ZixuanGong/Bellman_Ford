import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class Packet {
	private Client destClient;
	private HashMap<String, DistanceVector> dvMap;
	private byte[] pkt_bytes;
	
	public Packet(Client destClient, HashMap<String, DistanceVector> dvMap) {
		this.destClient = destClient;
		this.dvMap = dvMap;
	}
	
	public Packet() {
		
	}
	
	public void packPkt() {
		try {
			pkt_bytes = serialize(dvMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void unpackPkt() {
		try {
			HashMap<String, DistanceVector> tmp = (HashMap<String, DistanceVector>) deserialize(pkt_bytes);
			dvMap = tmp;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public byte[] getPkt_bytes() {
		return pkt_bytes;
	}

	public void setPkt_bytes(byte[] pkt_bytes) {
		this.pkt_bytes = pkt_bytes;
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
