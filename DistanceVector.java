import java.io.Serializable;
import java.util.ArrayList;

public class DistanceVector implements Serializable{
	private Client dest;
	private Client link;
	private float cost;
	
	
	public DistanceVector(Client dest, Client link, float cost) {
		this.dest = dest;
		this.link = link;
		this.cost = cost;
	}

	public Client getDest() {
		return dest;
	}

	public void setDest(Client dest) {
		this.dest = dest;
	}

	public Client getLink() {
		return link;
	}

	public void setLink(Client link) {
		this.link = link;
	}

	public float getCost() {
		return cost;
	}

	public void setCost(float cost) {
		this.cost = cost;
	}

	public String toString() {
		String dv = "";
		if (link != null)
			dv += link.toKey() + " ";

		dv += cost;
		return dv;
	}

	
	
	
	
}

