import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.HashMap;


public class Packet implements Serializable {
	public static final int LINKDOWN = 1;
	public static final int LINKUP = 2;
	public static final int UPDATE = 3;
	
	private Client destClient;
	private HashMap<String, DistanceVector> dvMap;
	// private byte[] pkt_bytes;
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
//		try {
//			if (dvMap != null) {
//				byte[] tmpMap = serialize(dvMap);
//				pkt_bytes = new byte[Array.getLength(tmpMap) + 4];
//				byte[] tmpType = ByteBuffer.allocate(4).putInt(msg_type).array();
//				System.arraycopy(tmpType, 0, pkt_bytes, 0, 4);
//				System.arraycopy(tmpMap, 0, pkt_bytes, 4, Array.getLength(tmpType));
//				System.out.println("in pack, pkt_bytes length=" + Array.getLength(pkt_bytes));
//			} else {
//				pkt_bytes = new byte[4];
//				byte[] tmp = ByteBuffer.allocate(4).putInt(msg_type).array();
//				System.arraycopy(tmp, 0, pkt_bytes, 0, 4);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
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
//		try {
//			System.out.println("in unpack, pkt_bytes len=" + Array.getLength(pkt_bytes));
//			byte[] tmpType = new byte[4];
//			
//			System.arraycopy(pkt_bytes, 0, tmpType, 0, 4);
//			ByteBuffer buf = ByteBuffer.wrap(tmpType);
//			msg_type = buf.getInt();
//			
//			if (msg_type == UPDATE) {
//				System.out.println("in unpack, =update");
//				byte[] tmp2 = new byte[Array.getLength(pkt_bytes)-4];
//				System.arraycopy(pkt_bytes, 4, tmp2, 0, Array.getLength(tmp2));
//				
//				dvMap = (HashMap<String, DistanceVector>) deserialize(tmp2);
//			}
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
		Packet pkt = null;
		try {
			pkt = (Packet) deserialize(pkt_bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
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

	// public byte[] getPkt_bytes() {
	// 	return pkt_bytes;
	// }

	// public void setPkt_bytes(byte[] pkt_bytes) {
	// 	this.pkt_bytes = pkt_bytes;
	// }

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
